/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.printflow.lite.core.jpa.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.printflow.lite.core.SpException;
import org.printflow.lite.core.SpInfo;
import org.printflow.lite.core.config.ConfigManager;
import org.printflow.lite.core.config.IConfigProp.Key;
import org.printflow.lite.core.dao.impl.DaoContextImpl;
import org.printflow.lite.core.jpa.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for comparing the schema version of the database
 * with that of the application. If the schema version of the application is
 * greater, then the database is incrementally upgraded using the upg-*.sql
 * scripts from <code>[server-home]/lib/sql/Derby/upgrades/</code>
 *
 * @author Rijk Ravestein
 *
 */
public class DbUpgManager {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DbUpgManager.class);

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link DbUpgManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final DbUpgManager INSTANCE = new DbUpgManager();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static DbUpgManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private DbUpgManager() {
    }

    /**
     * Reads a config property value directly from the database using a
     * separate EntityManager. This bypasses the config cache which may not
     * be populated yet at startup time.
     *
     * @param key
     *            The config property key.
     * @return The property value, or empty string if not found.
     * @throws Exception
     *                   If the table does not exist or query fails.
     */
    private String readConfigValueFromDb(final Key key) throws Exception {
        final String propName = ConfigManager.instance().getConfigKey(key);
        LOGGER.debug("DbUpgManager: Reading config property [{}] from database...", propName);
        final EntityManager em = ConfigManager.instance().createEntityManager();
        try {
            final Query query = em.createNativeQuery(
                    "SELECT property_value FROM tbl_config"
                            + " WHERE property_name = :name");
            query.setParameter("name", propName);
            final Object result = query.getSingleResult();
            if (result == null) {
                LOGGER.debug("DbUpgManager: Config property [{}] not found (null result)", propName);
                return "";
            }
            LOGGER.debug("DbUpgManager: Config property [{}] = [{}]", propName, result.toString());
            return result.toString();
        } finally {
            em.close();
        }
    }

    /**
     * Reads the schema version directly from the database using a separate
     * EntityManager. This bypasses the config cache which may not be populated
     * yet at startup time.
     *
     * @return The schema version as a long value.
     * @throws Exception
     *                   If the table does not exist or schema version not found.
     */
    private long readSchemaVersionFromDb() throws Exception {
        final String value = readConfigValueFromDb(Key.SYS_SCHEMA_VERSION);
        if (value.isEmpty()) {
            throw new SpException("Schema version is null or empty");
        }
        return Long.parseLong(value);
    }

    /**
     * Checks if the schema version of the database differs from schema version
     * this application supports.
     * <p>
     * If DB version Less Than App version, then incremental updates are
     * performed.
     * </p>
     * <p>
     * An exception is thrown when DB version is Greater Than App version.
     * </p>
     * <p>
     * If the database is empty (no tables), the schema is automatically created.
     * </p>
     *
     * @throws IOException
     *                     When "before-upgrade" backup failed.
     */
    public void check() throws IOException {

        LOGGER.info("DbUpgManager.check(): Starting database schema version check...");
        LOGGER.debug("DbUpgManager: Database JDBC URL: {}", ConfigManager.instance().getJdbcInfo().getUrl());
        LOGGER.debug("DbUpgManager: Database dialect: {}", ConfigManager.instance().getHibernateInfo().getDialect());

        /*
         * At this point we must depend on database access for the config keys.
         * If the database is empty (no tables yet), initDb() will create the schema.
         *
         * NOTE: The config cache is NOT yet populated at this point, so we
         * use a separate EntityManager to query the schema version directly
         * from the database. This avoids corrupting the main transaction.
         */
        final long schemaVersionDb;
        try {
            schemaVersionDb = readSchemaVersionFromDb();
            LOGGER.info("DbUpgManager: Found existing database schema version: {}", schemaVersionDb);
        } catch (Exception e) {
            LOGGER.info("Database appears to be empty or not initialized (exception: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "), initializing schema...");
            try {
                DbTools.initDb(new DbProcessListener() {
                    @Override
                    public void onLogEvent(String message) {
                        LOGGER.info(message);
                    }
                }, true);
                SpInfo.instance().log("Database schema created successfully.");
            } catch (Exception initEx) {
                LOGGER.error("Failed to initialize database schema: "
                        + initEx.getMessage(), initEx);
                throw new SpException("Failed to initialize database schema", initEx);
            }
            return;
        }

        final long schemaVersionApp = Long.parseLong(DbTools.getAppSchemaVersion());

        if (schemaVersionDb > schemaVersionApp) {
            throw new SpException("Current database version [" + schemaVersionDb
                    + "] is more recent than version [" + schemaVersionApp
                    + "] as supported by the application "
                    + "you are running now.  "
                    + "This normally occurs when you have downgraded"
                    + " the application.  Please restore a database"
                    + " backup from an application with a version "
                    + "number equal or prior from the one you are "
                    + "running now. Contact support for assistance.");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current database version [" + schemaVersionDb
                    + "] Application supports version [" + schemaVersionApp
                    + "]");
        }

        final ConfigManager cm = ConfigManager.instance();
        final String minorApp = DbTools.getAppSchemaVersionMinor();

        if (schemaVersionDb < schemaVersionApp) {

            SpInfo.instance()
                    .log("Database upgrade required from schema version ["
                            + schemaVersionDb + "] to [" + schemaVersionApp
                            + "]");

            /*
             * Make a snapshot of the database before the upgrade?
             */
            if (cm.isDbBackupBeforeUpg()) {
                final Date dateExport = new Date();
                final File backupFile = DbTools.exportDbBeforeUpg(
                        DaoContextImpl.peekEntityManager(),
                        cm.getConfigInt(Key.DB_EXPORT_QUERY_MAX_RESULTS),
                        schemaVersionDb, dateExport);
                SpInfo.instance().log("Backup file created before update: "
                        + backupFile.getAbsolutePath());
            } else {
                SpInfo.instance().log("No backup file created before update.");
            }
            /*
             * Do the upgrade.
             */
            upgrade(schemaVersionDb, schemaVersionApp);

            cm.updateConfigKey(Key.SYS_SCHEMA_VERSION_MINOR, minorApp,
                    Entity.ACTOR_SYSTEM);

        } else {
            String minorDb;
            try {
                minorDb = readConfigValueFromDb(
                        Key.SYS_SCHEMA_VERSION_MINOR);
            } catch (Exception ex) {
                minorDb = "";
            }

            if (minorDb.equals(minorApp)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Database schema is up-to-date");
                }
            } else {
                throw new IllegalStateException(
                        "Database minor version mismatch: " + "application ["
                                + minorApp + "] - database [" + minorDb + "]");
            }
        }
    }

    /**
     * Performs the incremental upgrades to the schema version of this
     * application.
     *
     * @param versionFrom
     *                    The version to upgrade from.
     * @param versionTo
     *                    The version to upgrade to.
     */
    private void upgrade(final long versionFrom, final long versionTo) {

        final long schemaVersionApp = Long.parseLong(DbTools.getAppSchemaVersion());

        long versionWlk = versionFrom;

        try {

            while (versionWlk < versionTo) {
                versionWlk++;
                doSchemaUpgrade(versionWlk);
            }

        } catch (Exception e) {
            LOGGER.error("Error occurred attempting to upgrade database: "
                    + e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        }

        /*
         * Did we executed all the required upgrade steps.
         */
        if (versionWlk != schemaVersionApp) {
            final String msg = "Database schema upgrade from [" + versionFrom
                    + "] to [" + schemaVersionApp
                    + "] failed: no upgrades implemented "
                    + "after upgrade to version [" + versionWlk + "].";
            throw new SpException(msg);
        }

    }

    /**
     * Performs the single upgrade.
     *
     * @param targetVersion
     *                      The schema version to upgrade to.
     */
    private void doSchemaUpgrade(final long targetVersion) {

        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Upgrading to schema version: " + targetVersion);
            }
            runSqlScript(DaoContextImpl.peekEntityManager(),
                    getDbUpgradeScript(String.valueOf(targetVersion)));

            ConfigManager.instance().updateConfigKey(Key.SYS_SCHEMA_VERSION,
                    targetVersion, Entity.ACTOR_SYSTEM);

            SpInfo.instance()
                    .log("Upgraded to schema version [" + targetVersion + "]");

        } catch (Exception e) {
            throw new SpException(
                    "Error occurred trying to upgrade to schema version: "
                            + targetVersion,
                    e);
        }
    }

    /**
     *
     * @param schemaVersion
     * @return
     */
    private File getDbUpgradeScript(final String schemaVersion) {
        return Paths
                .get(ConfigManager.getDbScriptDir().getAbsolutePath(),
                        "upgrades", String.format("upg-%s.sql", schemaVersion))
                .toFile();
    }

    /**
     * Runs statements from an SQL script file.
     *
     * @param em
     * @param sqlFile
     *                The SQL script file.
     */
    public static void runSqlScript(final EntityManager em,
            final File sqlFile) {

        String sqlFileName = sqlFile.getAbsolutePath();

        if (!sqlFile.exists()) {
            throw new SpException("Cannot find SQL file: " + sqlFileName);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Running script: " + sqlFileName);
        }

        BufferedReader reader = null;
        List<String> statements = new ArrayList<>();

        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(sqlFileName)));

            final StringBuilder statement = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (!line.trim().isEmpty() && (!line.startsWith("--"))) {

                    if (line.endsWith(";")) {
                        statement.append(line.substring(0, line.length() - 1));
                        statements.add(statement.toString());

                        statement.setLength(0);
                        continue;
                    }
                    statement.append(line).append(' ');
                }
            }
        } catch (IOException e) {
            throw new SpException(
                    "Unable to read database script: " + sqlFileName);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        /*
         *
         */
        for (String statement : statements) {
            runSqlStatement(em, statement);
        }
    }

    /**
     * Runs statements from an SQL script file.
     *
     * @param em
     * @param sqlFile
     *                The SQL script file.
     */
    public static void runSqlStatement(final EntityManager em,
            final String statement) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Running statement: " + statement);
        }

        try {
            /*
             * IMPORTANT: JPA cannot be used. Since the statements are NATIVE to
             * the database we need to create Native Queries.
             */
            Query query = em.createNativeQuery(statement);
            int affected = query.executeUpdate();

            if (LOGGER.isInfoEnabled()) {
                if (affected > 0) {
                    LOGGER.info("Rows affected: " + affected);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error running DB statement [" + statement + "] "
                    + e.toString());
            throw e;
        }
    }
}
