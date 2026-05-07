/*
 * This file is part of the PrintFlowLite project <https://github.com/YOUR_GITHUB/PrintFlowLite>.
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
package org.printflow.lite.client;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.printflow.lite.common.ConfigDefaults;
import org.printflow.lite.common.SystemPropertyEnum;
import org.printflow.lite.common.VersionInfo;
import org.printflow.lite.common.dto.ClientAppConnectDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PrintFlowLite Client Application.
 * <p>
 * References:
 * <ul>
 * <li><a href=
 * "http://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html"> How to
 * Use the System Tray</a></li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ClientApp implements UserEventClientListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientApp.class);

    /** */
    public static final String SECURE_URL_PROTOCOL = "https";

    /**
     * Width of the tray icon.
     */
    private static final int TRAYICON_WIDTH_24 = 24;

    /**
     * File name prefix of icon for "connected" state.
     */
    private static final String ICON_PFX_CONNECTED = "connected";

    /**
     * File name prefix of icon for "disconnected" state.
     */
    private static final String ICON_PFX_DISCONNECTED = "disconnected";

    /**
     * File name prefix of icon for "error" state.
     */
    private static final String ICON_PFX_ERROR = "error";

    /**
     * Environment variable holding the admin passkey.
     */
    private static final String ENV_ADMIN_PASSKEY =
            "PRINTFLOWLITE_CLIAPP_ADMIN_PASSKEY";

    /**
     * Name of the properties file.
     */
    private static final String FILENAME_CLIENT_PROPERTIES =
            "client.properties";

    /**
     * The name of relative {@code app} directory.
     */
    private static final String CONFIG_DIR_RELATIVE_APP = "app";

    /**
     * The name of relative {@code config} directory.
     */
    private static final String CONFIG_DIR_RELATIVE_CONFIG = "config";

    /**
     * Title of message box.
     */
    private static final String MSG_BOX_TITLE = ConfigDefaults.APP_NAME;

    /**
     * .
     */
    private static final int MSG_FONT_SIZE = 13;

    // ........................................................
    // Command-line options
    // ........................................................

    /**
     * Width (number of characters) of the usage text block in GUI pop-up.
     */
    private static final int HELP_FORMATTER_WIDTH_GUI = 90;

    /**
     * Width (number of characters) of the usage text block in TUI.
     */
    private static final int HELP_FORMATTER_WIDTH_TUI = 85;

    /** */
    private static final String CLI_SWITCH_HELP = "h";
    /** */
    private static final String CLI_SWITCH_HELP_LONG = "help";

    /** */
    private static final String CLI_SWITCH_HELP_TUI = "help-tui";

    /** */
    private static final String CLI_OPTION_PROPERTIES_FILE = "properties";

    /** */
    private static final String CLI_OPTION_SERVER_HOST = "server-host";

    /** */
    private static final String CLI_OPTION_SERVER_PORT = "server-port";

    /** */
    private static final String CLI_OPTION_USER = "user";

    /** */
    private static final String CLI_OPTION_PASSKEY = "passkey";

    /** */
    private static final String CLI_SWITCH_DEBUG = "d";
    /** */
    private static final String CLI_SWITCH_DEBUG_LONG = "debug";

    /** */
    private static final String CLI_OPTION_LOGDIR = "log-dir";

    /** */
    private static final String CLI_SWITCH_HIDE_EXIT = "x";
    /** */
    private static final String CLI_SWITCH_HIDE_EXIT_LONG = "hide-exit";

    /** */
    private static final String CLI_SWITCH_PRINTIN_DLG = "p";
    /** */
    private static final String CLI_SWITCH_PRINTIN_DLG_LONG = "print-dialog";

    /** */
    private static final String CLI_SWITCH_NOTIFY_SEND_LONG = "notify-send";

    /** */
    private static final String CLI_OPTION_ANCHOR_LONG = "anchor";

    /** */
    private static final String CLI_OPTION_PRINTIN_DLG_BTN = "print-dialog-btn";

    /** */
    private static final String CLI_OPTION_PRINTIN_URL_QUERY =
            "print-url-query";

    // ........................................................
    // Exit codes
    // ........................................................

    /** */
    public static final int EXIT_CODE_OK = 0;
    /** */
    public static final int EXIT_CODE_MISSING_PARMS = 1;
    /** */
    public static final int EXIT_CODE_PARMS_PARSE_ERROR = 2;
    /** */
    public static final int EXIT_CODE_ALREADY_RUNNING = 5;
    /** */
    public static final int EXIT_CODE_EXCEPTION = 9;
    /** */
    public static final int EXIT_CODE_SHOW_HELP = 255;

    /**
     *
     */
    private static final ClientAppVmArgs VM_ARGS = new ClientAppVmArgs();

    // ........................................................
    // User messages.
    // ........................................................
    /** */
    private static final String USER_MSG_CONNECTED = "You are connected!";
    /** */
    private static final String USER_MSG_RECONNECTING = "Reconnecting...";
    /** */
    private static final String USER_MSG_BYE = "Bye";
    /** */
    private static final String USER_MSG_PRINT_JOB_FINISHED =
            "Your PrintFlowLite Print Job has finished.";

    /** */
    private static final String USER_MENUITEM_WEBAPP = "Open WebApp...";
    /** */
    private static final String USER_MENUITEM_ABOUT = "About";
    /** */
    private static final String USER_MENUITEM_EXIT = "Exit";

    /**
     *
     */
    private static final String OS_NAME =
            SystemPropertyEnum.OS_NAME.getValue().toLowerCase();

    /**
     * The property for this application as read from file.
     */
    private Properties clientProps;

    /**
     *
     */
    private UserEventClient userEventclient;

    /**
     *
     */
    private JButton appStatusButton;

    /**
     * Use 'notify-send' command to send desktop notifications (Linux only).
     */
    private boolean notifySend;

    /**
     *
     */
    private TrayIcon trayIcon;

    /**
     * A toggle to indicatie a print-in notification message was received.
     */
    private boolean printInNotificationToggle;

    /**
     *
     */
    private String serverHost;

    /**
     *
     */
    private String serverSslPort;

    /**
     *
     */
    private URI webAppUri;

    /**
     *
     */
    private URI webAppUriPrintIn;

    /**
     *
     */
    private String webAppQueryPrintIn;

    /**
     *
     */
    private final Image imageDisconnected;

    /**
     *
     */
    private final Image imageError;

    /**
     *
     */
    private final Image imageConnected;

    /**
     *
     */
    private boolean hasExitMenuItem;

    /**
     * .
     */
    private boolean showPrintInActionDlg;

    /**
     * .
     */
    private String printInActionButton;

    /**
     *
     */
    private AppAnchorEnum appAnchor;

    /**
     * Menu items used in pop-up of (tray) application.
     */
    private enum MenuItemEnum {
        /** */
        WEBAPP,
        /** */
        ABOUT,
        /** */
        EXIT
    }

    /**
     *
     */
    private enum AppAnchorEnum {
        /** North East. */
        NE,
        /** North West. */
        NW,
        /** South East. */
        SE,
        /** South West. */
        SW
    }

    /**
     * Opener of the User Web App.
     *
     * @author Rijk Ravestein
     *
     */
    private static final class UserWebAppOpener implements ActionListener {

        /**
         * The parent app.
         */
        private final ClientApp clientApp;

        /**
         *
         * @param app
         *            The parent app.
         */
        private UserWebAppOpener(final ClientApp app) {
            this.clientApp = app;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            clientApp.openUserWebApp();
        }
    };

    /**
     * Default constructor.
     */
    private ClientApp() {

        this.imageDisconnected =
                createImage(getTrayIconPath(ICON_PFX_DISCONNECTED));
        this.imageError = createImage(getTrayIconPath(ICON_PFX_ERROR));
        this.imageConnected = createImage(getTrayIconPath(ICON_PFX_CONNECTED));
    }

    /**
     *
     * @return The name of the user owning the current process.
     */
    public static String getSysPropUserName() {
        return SystemPropertyEnum.USER_NAME.getValue();
    }

    /**
     *
     * @return The application version string.
     */
    private static String getAppVersion() {
        return String.format("%s %s", VM_ARGS.getAppName(), getVersion());
    }

    /**
     *
     * @return The application version string.
     */
    private static String getVersion() {
        return String.format("%s.%s.%s%s.%s", VersionInfo.VERSION_A_MAJOR,
                VersionInfo.VERSION_B_MINOR, VersionInfo.VERSION_C_REVISION,
                VersionInfo.VERSION_D_STATUS, VersionInfo.VERSION_E_BUILD);
    }

    /**
     * Shows usage and options of program on in {@link JOptionPane}.
     *
     * @param options
     *            The command line options.
     * @param isGui
     *            {@code true} when help text is to be displayed in Swing
     *            Message Dialog.
     */
    private void usage(final org.apache.commons.cli.Options options,
            final boolean isGui) {

        final org.apache.commons.cli.HelpFormatter formatter =
                new org.apache.commons.cli.HelpFormatter();

        final ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(ostr);

        final String footer = String.format(
                "\nLocations.\nApplication : %s\nLock file   : %s",
                VM_ARGS.getAppHome(), ClientAppLock.getLockDir());

        final int helpWidth;
        if (isGui) {
            helpWidth = HELP_FORMATTER_WIDTH_GUI;
        } else {
            helpWidth = HELP_FORMATTER_WIDTH_TUI;
        }

        formatter.printHelp(writer, helpWidth,
                VM_ARGS.getAppName() + " <options>", "", options, 3, 3, footer);

        writer.flush();

        if (isGui) {

            final String fontKey = "OptionPane.messageFont";

            final Object fontSaved = UIManager.get(fontKey);

            UIManager.put(fontKey, new FontUIResource(
                    new Font("Courier", Font.PLAIN, MSG_FONT_SIZE)));

            JOptionPane.showMessageDialog(null, ostr.toString(),
                    getAppVersion() + " on " + OS_NAME,
                    JOptionPane.INFORMATION_MESSAGE);

            UIManager.put(fontKey, fontSaved);

        } else {
            System.out.println(ostr.toString());
        }

    }

    /**
     *
     * @return The {@link Options}.
     */
    private Options createCliOptions() {

        final Options options = new Options();

        //
        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Display help text in GUI.");

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_HELP_TUI)
                        .desc("Display help text in TUI.").build());

        options.addOption(CLI_SWITCH_DEBUG, CLI_SWITCH_DEBUG_LONG, false,
                "Write debug messages to the log file.");

        options.addOption(CLI_SWITCH_HIDE_EXIT, CLI_SWITCH_HIDE_EXIT_LONG,
                false,
                "Hide the \"Exit\" menuitem (optional). Default: false.");

        options.addOption(CLI_SWITCH_PRINTIN_DLG, CLI_SWITCH_PRINTIN_DLG_LONG,
                false, "Show action dialog at print-in event.");

        options.addOption(Option.builder().hasArg(true)
                .longOpt(CLI_OPTION_PRINTIN_DLG_BTN)
                .desc("Button text on print-in action dialog for opening "
                        + "User Web App (optional).")
                .build());

        options.addOption(Option.builder().hasArg(true)
                .longOpt(CLI_OPTION_PRINTIN_URL_QUERY)
                .desc("URL query for opening User Web App at "
                        + "print-in event (optional).")
                .build());

        options.addOption(Option.builder().hasArg(true).argName("dir")
                .longOpt(CLI_OPTION_LOGDIR)
                .desc(String.format("Log file directory. Default: %s",
                        loggerHome()))
                .build());

        options.addOption(Option.builder().hasArg(true).argName("file")
                .longOpt(CLI_OPTION_PROPERTIES_FILE)
                .desc(String.format(
                        "File with default command-line options "
                                + "(optional). Default: %s",
                        getDefaultPropertiesFile()))
                .build());

        options.addOption(
                Option.builder().hasArg(true).longOpt(CLI_OPTION_SERVER_HOST)
                        .desc("IP address or name of PrintFlowLite server").build());

        options.addOption(Option.builder().hasArg(true).argName("number")
                .longOpt(CLI_OPTION_SERVER_PORT)
                .desc(String.format(
                        "SSL port of PrintFlowLite server (optional). "
                                + "Default: %s.",
                        ConfigDefaults.SERVER_SSL_PORT))
                .build());

        options.addOption(
                Option.builder().hasArg(true).argName("name")
                        .longOpt(CLI_OPTION_USER)
                        .desc(String.format(
                                "A different username than current user "
                                        + "\"%s\" (optional).",
                                getSysPropUserName()))
                        .build());

        options.addOption(Option.builder().hasArg(true).argName("key")
                .longOpt(CLI_OPTION_PASSKEY)
                .desc("The admin passkey (optional).").build());

        options.addOption(
                Option.builder().hasArg(false)
                        .longOpt(CLI_SWITCH_NOTIFY_SEND_LONG)
                        .desc("Use 'notify-send' command to send "
                                + "desktop notifications (Linux only).")
                        .build());

        final StringBuilder argName = new StringBuilder();
        for (final AppAnchorEnum value : AppAnchorEnum.values()) {
            if (argName.length() > 0) {
                argName.append('|');
            }
            argName.append(value.toString().toLowerCase());
        }

        options.addOption(
                Option.builder().hasArg(true).argName(argName.toString())
                        .longOpt(CLI_OPTION_ANCHOR_LONG)
                        .desc(String.format(
                                "Show on desktop at anchor position instead of "
                                        + "tray. --%s switch is auto activated "
                                        + "(Linux only).",
                                CLI_SWITCH_NOTIFY_SEND_LONG))
                        .build());

        return options;
    }

    /**
     *
     * @return The default property file.
     */
    private static File getDefaultPropertiesFile() {
        return FileSystems.getDefault()
                .getPath(VM_ARGS.getAppHome(), CONFIG_DIR_RELATIVE_APP,
                        CONFIG_DIR_RELATIVE_CONFIG, FILENAME_CLIENT_PROPERTIES)
                .toFile();
    }

    /**
     * Reads the client properties from file.
     *
     * @param file
     *            The file.
     * @return the {@link Properties}.
     * @throws IOException
     *             When file cannot be opened or read.
     */
    private static Properties readClientProperties(final File file)
            throws IOException {

        final Properties serverProps = new Properties();

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            serverProps.load(fis);
            return serverProps;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Creates a trust manager that does not validate certificate chains.
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static void disableSslVerification()
            throws NoSuchAlgorithmException, KeyManagementException {

        final TrustManager[] trustAllCerts =
                new TrustManager[] { new X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[]
                            getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(
                            final X509Certificate[] certs,
                            final String authType) {
                    }

                    @Override
                    public void checkServerTrusted(
                            final X509Certificate[] certs,
                            final String authType) {
                    }
                } };

        /*
         * Install the all-trusting trust manager.
         */
        final SSLContext sc = SSLContext.getInstance("SSL");

        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        /*
         * Create all-trusting host name verifier.
         */
        final HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname,
                    final SSLSession session) {
                return true;
            }
        };

        /*
         * Install the all-trusting host verifier.
         */
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    /**
     *
     * @return The directory where log file is written.
     */
    private static String loggerHome() {
        return SystemPropertyEnum.USER_HOME.getValue();
    }

    /**
     * Creates a log4j configuration that logs to file.
     *
     * @param location
     *            The location (directory) of the log file.
     * @param applicationId
     * @param level
     * @throws IOException
     */
    private void createFileLogger(final File location,
            final String applicationId, final Level level) throws IOException {

        final File logFile = FileSystems.getDefault()
                .getPath(location.getCanonicalPath(), applicationId + ".log")
                .toFile();

        PatternLayout layout =
                new PatternLayout("%d{ISO8601} %5p %c{1}:%L - %m [%t]%n");

        final RollingFileAppender appender;
        try {
            appender = new RollingFileAppender(layout,
                    logFile.getCanonicalPath(), true);
        } catch (IOException e) {
            throw new ClientAppException(e.getMessage());
        }

        appender.setThreshold(level);
        appender.setName("FileAppender");
        appender.setEncoding("UTF8");
        appender.setMaxBackupIndex(5);
        appender.setMaxFileSize("10MB");

        BasicConfigurator.configure(appender);
    }

    /**
     * Runs the application.
     *
     * @param args
     *            The arguments.
     * @return The exit code.
     * @throws ParseException
     *             When syntax error in command-line options.
     * @throws IOException
     */
    private int run(final String[] args) throws ParseException, IOException {

        // ......................................................
        // Parse parameters from CLI
        // ......................................................
        Options options = createCliOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            showErrorMessage(e.getMessage());
            usage(options, true);
            return EXIT_CODE_PARMS_PARSE_ERROR;
        }

        // ......................................................
        // Help needed?
        // ......................................................
        if (cmd.hasOption(CLI_SWITCH_HELP)
                || cmd.hasOption(CLI_SWITCH_HELP_LONG)) {
            usage(options, true);
            return EXIT_CODE_SHOW_HELP;
        }

        if (cmd.hasOption(CLI_SWITCH_HELP_TUI)) {
            usage(options, false);
            return EXIT_CODE_SHOW_HELP;
        }

        // ......................................................
        // Get the properties file
        // ......................................................
        final File fileProps;

        if (cmd.hasOption(CLI_OPTION_PROPERTIES_FILE)) {
            fileProps =
                    new File(cmd.getOptionValue(CLI_OPTION_PROPERTIES_FILE));
        } else {
            fileProps = getDefaultPropertiesFile();
        }

        this.clientProps = readClientProperties(fileProps);

        // ...................................................................
        // Check parameters.
        // ...................................................................
        String optionWlk;

        //
        optionWlk = CLI_OPTION_ANCHOR_LONG;

        final String valueAnchorTmp;
        if (cmd.hasOption(optionWlk)) {
            valueAnchorTmp = cmd.getOptionValue(optionWlk);
        } else {
            valueAnchorTmp = this.clientProps.getProperty(optionWlk);
        }

        // Apply default.
        final String valueAnchor;
        if (valueAnchorTmp == null && !SystemTray.isSupported()) {
            valueAnchor = AppAnchorEnum.NE.toString().toLowerCase();
        } else {
            valueAnchor = valueAnchorTmp;
        }

        final boolean isTrayApp = valueAnchor == null;

        if (!isTrayApp && !isLinux()) {
            showErrorMessage(
                    String.format("Option '%s' is GNU/Linux only.", optionWlk));
            return EXIT_CODE_MISSING_PARMS;
        }

        if (!isTrayApp) {
            try {
                this.appAnchor =
                        AppAnchorEnum.valueOf(valueAnchor.toUpperCase());
            } catch (Exception e) {
                showErrorMessage(
                        String.format("Option '%s' : value '%s' is invalid.",
                                optionWlk, valueAnchor));
                return EXIT_CODE_EXCEPTION;
            }
            this.notifySend = true;
        }

        //
        if (!this.notifySend) {
            optionWlk = CLI_SWITCH_NOTIFY_SEND_LONG;

            if (cmd.hasOption(optionWlk)) {
                this.notifySend = true;
            } else {
                this.notifySend = this.clientProps
                        .getProperty(CLI_SWITCH_NOTIFY_SEND_LONG, "false")
                        .toLowerCase().equals("true");
            }
        }

        if (this.notifySend && !isLinux()) {
            showErrorMessage(
                    String.format("Option '%s' is GNU/Linux only.", optionWlk));
            return EXIT_CODE_MISSING_PARMS;
        }

        //
        optionWlk = CLI_OPTION_SERVER_HOST;

        if (cmd.hasOption(optionWlk)) {
            this.serverHost = cmd.getOptionValue(optionWlk);
        } else {
            this.serverHost = this.clientProps.getProperty(optionWlk);
        }

        if (this.serverHost == null || this.serverHost.trim().isEmpty()) {
            showErrorMessage(String.format("Option missing: %s", optionWlk));
            return EXIT_CODE_MISSING_PARMS;
        }

        //
        optionWlk = CLI_OPTION_SERVER_PORT;

        if (cmd.hasOption(optionWlk)) {
            this.serverSslPort = cmd.getOptionValue(optionWlk);
        } else {
            this.serverSslPort = this.clientProps.getProperty(optionWlk,
                    ConfigDefaults.SERVER_SSL_PORT);
        }

        if (this.serverSslPort == null) {
            showErrorMessage(String.format("Option missing: %s", optionWlk));
            return EXIT_CODE_MISSING_PARMS;
        }

        //
        optionWlk = CLI_OPTION_USER;

        final String user;

        if (cmd.hasOption(optionWlk)) {

            user = cmd.getOptionValue(optionWlk);

        } else {
            user = this.clientProps.getProperty(optionWlk,
                    SystemPropertyEnum.USER_NAME.getValue());
        }

        if (user == null) {
            showErrorMessage(String.format("Option missing: %s", optionWlk));
            return EXIT_CODE_MISSING_PARMS;
        }

        //
        optionWlk = CLI_OPTION_PASSKEY;

        final String passKey;

        if (cmd.hasOption(optionWlk)) {
            passKey = cmd.getOptionValue(optionWlk);
        } else {
            passKey = System.getenv(ENV_ADMIN_PASSKEY);
        }

        // ......................................................
        // Check if application is already active.
        // ......................................................
        final String applicationId =
                String.format("%s_%s_%s_%s", VM_ARGS.getAppName(), user,
                        this.serverHost, this.serverSslPort);

        final ClientAppLock appLock = new ClientAppLock(applicationId);

        if (appLock.isAppActive()) {
            showErrorMessage("Application is already active.");
            return EXIT_CODE_ALREADY_RUNNING;
        }

        // ......................................................
        // File logger
        // ......................................................
        final Level logLevel;

        if (cmd.hasOption(CLI_SWITCH_DEBUG)
                || cmd.hasOption(CLI_SWITCH_DEBUG_LONG)) {
            logLevel = Level.INFO;
        } else {
            logLevel = Level.ERROR;
        }

        final File logLocation =
                new File(cmd.getOptionValue(CLI_OPTION_LOGDIR, loggerHome()));

        if (!logLocation.exists()) {
            throw new ClientAppException("Directory "
                    + logLocation.getCanonicalPath() + " does not exist.");
        }

        createFileLogger(logLocation, applicationId, logLevel);

        // ......................................................
        // Exit menuitem?
        // ......................................................
        if (cmd.hasOption(CLI_SWITCH_HIDE_EXIT)
                || cmd.hasOption(CLI_SWITCH_HIDE_EXIT_LONG)) {
            this.hasExitMenuItem = false;
        } else {
            this.hasExitMenuItem = this.clientProps
                    .getProperty(CLI_SWITCH_HIDE_EXIT_LONG, "false")
                    .toLowerCase().equals("false");
        }

        // ......................................................
        // Show PrintIn dialog?
        // ......................................................
        if (cmd.hasOption(CLI_SWITCH_PRINTIN_DLG)
                || cmd.hasOption(CLI_SWITCH_PRINTIN_DLG_LONG)) {
            this.showPrintInActionDlg = true;
        } else {
            this.showPrintInActionDlg = this.clientProps
                    .getProperty(CLI_SWITCH_PRINTIN_DLG_LONG, "false")
                    .toLowerCase().equals("true");
        }

        // ......................................................
        // URL query for print-in
        // ......................................................
        optionWlk = CLI_OPTION_PRINTIN_URL_QUERY;

        if (cmd.hasOption(optionWlk)) {
            this.webAppQueryPrintIn = cmd.getOptionValue(optionWlk);
        } else {
            this.webAppQueryPrintIn = this.clientProps.getProperty(optionWlk);
        }

        // ......................................................
        // Button text for print-in
        // ......................................................
        optionWlk = CLI_OPTION_PRINTIN_DLG_BTN;

        if (cmd.hasOption(optionWlk)) {
            this.printInActionButton = cmd.getOptionValue(optionWlk);
        } else {
            this.printInActionButton = this.clientProps.getProperty(optionWlk);
        }

        // ...................................................................
        // Go for it.
        // ...................................................................
        int ret = EXIT_CODE_EXCEPTION;

        try {
            /*
             * For future use...
             *
             * When using the javax.net.ssl.trustStore construct, do NOT use IP
             * address in URL, since this gives a
             * java.security.cert.CertificateException: No subject alternative
             * names present.
             *
             * But, use the CN as in the certificate "Owner: CN=", which is
             * "pampus" in our case.
             */

            // System.setProperty("javax.net.ssl.trustStore",
            // "path-to-truststore");

            disableSslVerification();

            this.userEventclient = new UserEventClient(serverHost,
                    serverSslPort, user, passKey, this);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

            runApp(isTrayApp);

            ret = EXIT_CODE_OK;

        } catch (Exception e) {
            showErrorMessage(e.getMessage());
        }

        return ret;
    }

    /**
     * @return {@code true} if we are running Windows.
     */
    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /**
     * @return {@code true} if we are running Mac OS.
     */
    @SuppressWarnings("unused")
    private static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    /**
     * @return {@code true} if we are running Linux.
     */
    private static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    /**
     * @return {@code true} if we are running Solaris.
     */
    @SuppressWarnings("unused")
    private static boolean isSolaris() {
        return OS_NAME.contains("solaris");
    }

    /**
     * @return {@code true} if we are running *nix.
     */
    @SuppressWarnings("unused")
    private static boolean isUnix() {
        return OS_NAME.contains("nix") || OS_NAME.contains("nux")
                || OS_NAME.contains("aix");
    }

    /**
     *
     * @return Relative path of application icon.
     */
    private static String getAppIconPath() {
        return "images/printflowlite-icon-72x72.png";
    }

    /**
     *
     * @param iconPfx
     *            The icon prefix.
     * @return The path of the icon.
     */
    private static String getTrayIconPath(final String iconPfx) {

        final String iconPath16x16 = "images/" + iconPfx + "-16x16.gif";

        if (!SystemTray.isSupported()) {
            return iconPath16x16;
        }

        final SystemTray systemTray = SystemTray.getSystemTray();
        final Dimension trayIconSize = systemTray.getTrayIconSize();

        final String iconPath;

        if (isWindows()) {
            iconPath = iconPath16x16;
        } else if (trayIconSize.width >= TRAYICON_WIDTH_24) {
            // Ubuntu (GNU/Linux): does not use transparency though ...
            iconPath = "images/" + iconPfx + "-24x24.gif";
        } else {
            iconPath = iconPath16x16;
        }

        return iconPath;
    }

    /**
     * Runs the application.
     *
     * @param isTrayApp
     *            When {@code true}, this is a Tray application.
     */
    private void runApp(final boolean isTrayApp) {

        try {
            /*
             * Set the Look and Feel of the application to the operating
             * system's look and feel.
             */
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException e) {
            throw new ClientAppException(e.getMessage());
        }

        /*
         * Turn off metal's use of bold fonts.
         */
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        /*
         * Schedule a job for the event-dispatching thread: adding TrayIcon.
         */
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI(isTrayApp);
            }
        });

    }

    /**
     * Opens the USer Web App.
     */
    private void openUserWebApp() {
        final URI uri;

        if (this.printInNotificationToggle) {
            uri = this.webAppUriPrintIn;
            // This is a one-shot, so reset the indicator.
            this.printInNotificationToggle = false;
        } else {
            uri = this.webAppUri;
        }

        ClientApp.openUserWebApp(uri);
    }

    /**
     *
     * @param webAppUri
     *            The URL of the Web App.
     */
    private static void openUserWebApp(final URI webAppUri) {

        if (webAppUri != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(webAppUri);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        MSG_BOX_TITLE, JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     *
     * @param items
     * @param listeners
     */
    private void addMenuItemListeners(final Map<MenuItemEnum, MenuItem> items,
            final Map<MenuItemEnum, ActionListener> listeners) {

        for (final MenuItemEnum key : items.keySet()) {
            if (listeners.containsKey(key)) {
                items.get(key).addActionListener(listeners.get(key));
            }
        }
    }

    /**
     *
     * @param items
     * @param listeners
     */
    private void addJMenuItemListeners(final Map<MenuItemEnum, JMenuItem> items,
            final Map<MenuItemEnum, ActionListener> listeners) {

        for (final MenuItemEnum key : items.keySet()) {
            if (listeners.containsKey(key)) {
                items.get(key).addActionListener(listeners.get(key));
            }
        }
    }

    /**
     * @return The map with {@link MenuItem} objects.
     */
    private Map<MenuItemEnum, MenuItem>
            createMenuItems(final Map<MenuItemEnum, String> items) {

        final Map<MenuItemEnum, MenuItem> map = new HashMap<>();

        for (final MenuItemEnum key : items.keySet()) {
            final MenuItem menuItem;
            if (items.get(key) == null) {
                menuItem = null;
            } else {
                menuItem = new MenuItem(items.get(key));
            }
            map.put(key, menuItem);
        }
        return map;
    }

    /**
     * @return The map with {@link JMenuItem} objects.
     */
    private Map<MenuItemEnum, JMenuItem>
            createJMenuItems(final Map<MenuItemEnum, String> items) {

        final Map<MenuItemEnum, JMenuItem> map = new HashMap<>();

        for (final MenuItemEnum key : items.keySet()) {
            final JMenuItem menuItem;
            if (items.get(key) == null) {
                menuItem = null;
            } else {
                menuItem = new JMenuItem(items.get(key));
            }
            map.put(key, menuItem);
        }
        return map;
    }

    /**
     *
     * @param items
     *            The menu items.
     * @param listeners
     *            The listeners.
     */
    private void createAndShowGUI(final Map<MenuItemEnum, String> items,
            final Map<MenuItemEnum, ActionListener> listeners) {

        final JFrame anchorFrame = new JFrame();

        final int imageWidth = 24;

        // Popup
        final JPopupMenu popup = new JPopupMenu();

        final Map<MenuItemEnum, JMenuItem> menuItems = createJMenuItems(items);

        this.addJMenuItemListeners(menuItems, listeners);

        popup.add(menuItems.get(MenuItemEnum.WEBAPP));
        popup.addSeparator();
        popup.add(menuItems.get(MenuItemEnum.ABOUT));

        if (menuItems.get(MenuItemEnum.EXIT) != null) {
            popup.addSeparator();
            popup.add(menuItems.get(MenuItemEnum.EXIT));
        }

        //
        anchorFrame.setIconImage(createImage(getAppIconPath()));

        final String header = MSG_BOX_TITLE;

        // Height of the task bar
        @SuppressWarnings("unused")
        final Insets screenInsets = Toolkit.getDefaultToolkit()
                .getScreenInsets(anchorFrame.getGraphicsConfiguration());

        final GridBagLayout layoutManager = new GridBagLayout();
        anchorFrame.setLayout(layoutManager);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0f;
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(1, 5, 1, 5);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHEAST;

        final JLabel headingLabel = new JLabel(header);

        headingLabel.setOpaque(false);

        anchorFrame.add(headingLabel, constraints);

        constraints.gridx++;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.fill = GridBagConstraints.NONE;

        //
        final MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openUserWebApp();
                }
            }
        };

        appStatusButton = new JButton();

        appStatusButton.addMouseListener(mouseAdapter);

        appStatusButton.setIcon(new ImageIcon(this.imageDisconnected));

        appStatusButton.setMargin(new Insets(0, 0, 0, 0));
        appStatusButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        appStatusButton.setFocusable(false);

        anchorFrame.add(appStatusButton, constraints);
        anchorFrame.addMouseListener(mouseAdapter);

        // Colors
        // anchorFrame.getContentPane().setBackground(Color.DARK_GRAY);
        // headingLabel.setForeground(Color.LIGHT_GRAY);

        /*
         * Position and else.
         */
        final Font font = new Font(null, Font.PLAIN, 12);

        headingLabel.setFont(font);

        final int headingWidth =
                anchorFrame.getFontMetrics(font).stringWidth(header);

        final int frameWidth = headingWidth + imageWidth
                + headingLabel.getInsets().left + headingLabel.getInsets().right
                + anchorFrame.getInsets().left + anchorFrame.getInsets().right
                + 20 + appStatusButton.getInsets().left
                + appStatusButton.getInsets().right;

        int frameHeight = constraints.insets.top + constraints.insets.bottom
                + anchorFrame.getInsets().top + anchorFrame.getInsets().bottom;

        final int headingHeight = anchorFrame.getFontMetrics(font).getHeight()
                + headingLabel.getInsets().top
                + headingLabel.getInsets().bottom;

        final int imageHeight = appStatusButton.getIcon().getIconHeight()
                + appStatusButton.getInsets().top
                + appStatusButton.getInsets().bottom;

        if (imageHeight > headingHeight) {
            frameHeight += imageHeight;
        } else {
            frameHeight += headingHeight;
        }

        anchorFrame.setSize(frameWidth, frameHeight);

        // Size of the screen.
        final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        switch (this.appAnchor) {
        case NE:
            anchorFrame.setLocation(scrSize.width - anchorFrame.getWidth(), 0);
            break;
        case NW:
            anchorFrame.setLocation(0, 0);
            break;
        case SE:
            anchorFrame.setLocation(scrSize.width - anchorFrame.getWidth(),
                    scrSize.height - anchorFrame.getHeight());
            break;
        case SW:
            anchorFrame.setLocation(0,
                    scrSize.height - anchorFrame.getHeight());
            break;

        default:
            break;
        }

        //
        final WindowAdapter windowAdapter = new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                super.windowClosing(e);
                anchorFrame.dispose();
            }

            @Override
            public void windowClosed(final WindowEvent e) {
                super.windowClosed(e);
                System.exit(0);
            }
        };

        // When you press "X" the WINDOW_CLOSING event is called
        anchorFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        anchorFrame.addWindowListener(windowAdapter);

        anchorFrame.setAlwaysOnTop(true);
        anchorFrame.setUndecorated(true);
        anchorFrame.setResizable(false);

        anchorFrame.setVisible(true);
    }

    /**
     * Creates and shows the main application.
     *
     * @param isTrayApp
     *            When {@code true}, this is a Tray application. Otherwise, this
     *            is an small window at the anchor position
     *            {@link AppAnchorEnum}.
     */
    private void createAndShowGUI(final boolean isTrayApp) {

        /*
         * Check the SystemTray support.
         */
        if (isTrayApp && !SystemTray.isSupported()) {
            throw new ClientAppException("SystemTray is not supported");
        }

        /*
         * Menu items.
         */
        final Map<MenuItemEnum, String> items = new HashMap<>();

        items.put(MenuItemEnum.WEBAPP, USER_MENUITEM_WEBAPP);
        items.put(MenuItemEnum.ABOUT, USER_MENUITEM_ABOUT);

        if (this.hasExitMenuItem) {
            items.put(MenuItemEnum.EXIT, USER_MENUITEM_EXIT);
        } else {
            items.put(MenuItemEnum.EXIT, null);
        }

        /*
         * Listeners.
         */
        final Map<MenuItemEnum, ActionListener> listeners = new HashMap<>();

        listeners.put(MenuItemEnum.WEBAPP, new UserWebAppOpener(this));

        listeners.put(MenuItemEnum.ABOUT, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                final String msg =
                        String.format("%s\r\nos.name: %s\r\nuser.name: %s",
                                getVersion(), OS_NAME, getSysPropUserName());

                JOptionPane.showMessageDialog(null, msg, MSG_BOX_TITLE,
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        if (this.hasExitMenuItem) {

            listeners.put(MenuItemEnum.EXIT, new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOGGER.info("Application closed by user.");
                    System.exit(EXIT_CODE_OK);
                }
            });
        }

        //
        if (isTrayApp) {

            appStatusButton = null;

            final PopupMenu popup = new PopupMenu();

            final Map<MenuItemEnum, MenuItem> menuItems =
                    createMenuItems(items);

            this.addMenuItemListeners(menuItems, listeners);

            popup.add(menuItems.get(MenuItemEnum.WEBAPP));
            popup.addSeparator();
            popup.add(menuItems.get(MenuItemEnum.ABOUT));

            if (menuItems.get(MenuItemEnum.EXIT) != null) {
                popup.addSeparator();
                popup.add(menuItems.get(MenuItemEnum.EXIT));
            }

            final SystemTray systemTray = SystemTray.getSystemTray();

            this.trayIcon = new TrayIcon(this.imageDisconnected);

            this.trayIcon.setToolTip(ConfigDefaults.APP_NAME);
            this.trayIcon.setImageAutoSize(true);

            // This action is triggered by a double-click on the tray-icon.
            this.trayIcon.addActionListener(listeners.get(MenuItemEnum.WEBAPP));

            this.trayIcon.setPopupMenu(popup);

            try {
                systemTray.add(this.trayIcon);
            } catch (AWTException e) {
                throw new ClientAppException("TrayIcon could not be added.");
            }

        } else {
            this.trayIcon = null;
            createAndShowGUI(items, listeners);
        }

        /*
         * Start connecting in a separate thread so the tray icon will display.
         */
        final TimerTask connectTask = new TimerTask() {

            @Override
            public void run() {
                startConnection();
            }
        };

        waitTimer.schedule(connectTask, 10);
    }

    /**
     * A static {@link Timer} so all the locks will use the same thread for the
     * wait timer.
     */
    private static Timer waitTimer = new Timer(true);

    /**
     * Starts the connection of the {@link UserEventClient} instance.
     */
    private void startConnection() {
        this.userEventclient.connect();
    }

    /**
     *
     */
    public void onShutdown() {
        userEventclient.disconnect();
        LOGGER.info("Bye.");
    }

    @Override
    public void onError(final String message) {
        this.onUserMessage(this.imageError, message,
                TrayIcon.MessageType.ERROR);
        LOGGER.error(message);
        userEventclient.disconnect();
    }

    @Override
    public void onConnected(final ClientAppConnectDto connectInfo) {

        if (this.printInActionButton == null) {
            this.printInActionButton = connectInfo.getPrintInActionButton();
        }

        try {
            this.webAppUri = new URL(SECURE_URL_PROTOCOL, this.serverHost,
                    Integer.parseInt(this.serverSslPort),
                    String.format("%s?%s", connectInfo.getWebAppPath(),
                            connectInfo.getWebAppQuery())).toURI();

            final String query;

            if (this.webAppQueryPrintIn == null) {
                query = connectInfo.getWebAppQueryPrintIn();
            } else {
                query = this.webAppQueryPrintIn;
            }

            if (query == null || query.trim().isEmpty()) {
                this.webAppUriPrintIn = this.webAppUri;
            } else {
                this.webAppUriPrintIn = new URL(String.format("%s&%s",
                        this.webAppUri.toString(), query.trim())).toURI();
            }

        } catch (URISyntaxException | MalformedURLException e) {
            this.webAppUri = null;
            this.onUserMessage(null, e.getMessage(),
                    TrayIcon.MessageType.ERROR);
        }

        if (this.webAppUri != null) {
            this.onUserMessage(this.imageConnected, USER_MSG_CONNECTED,
                    TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void onConnectionBroken() {
        this.onUserMessage(this.imageDisconnected, USER_MSG_RECONNECTING,
                TrayIcon.MessageType.WARNING);
        this.startConnection();
    }

    @Override
    public void onConnectionClosed() {
        this.onUserMessage(this.imageDisconnected, USER_MSG_BYE,
                TrayIcon.MessageType.INFO);
    }

    @Override
    public void onUserMessage(final String message) {
        this.onUserMessage(message, false);
    }

    /**
     *
     * @param message
     * @param isPrintInMsg
     */
    private void onUserMessage(final String message,
            final boolean isPrintInMsg) {
        this.printInNotificationToggle = isPrintInMsg;
        this.onUserMessage(null, message, TrayIcon.MessageType.INFO);
    }

    /**
     *
     * @param image
     *            If {@code null}, just the message is handled.
     * @param message
     *            The message text.
     * @param msgType
     *            The {@link TrayIcon.MessageType} is used, also when tray
     *            application is not used.
     */
    private void onUserMessage(final Image image, final String message,
            final TrayIcon.MessageType msgType) {

        if (this.trayIcon != null) {
            if (image != null) {
                this.trayIcon.setImage(image);
            }
            if (!this.notifySend) {
                this.trayIcon.displayMessage(MSG_BOX_TITLE, message, msgType);
            }
        }

        if (this.appStatusButton != null) {
            if (image != null) {
                this.appStatusButton.setIcon(new ImageIcon(image));
            }

        }

        if (this.notifySend) {
            sysNotifySend(message);
        }
    }

    /**
     *
     * @return The text for the action button.
     */
    private String resolvePrintInActionButton() {
        if (this.printInActionButton == null) {
            return "Open in browser";
        }
        return this.printInActionButton;
    }

    @Override
    public void onPrintIn() {

        final String message = USER_MSG_PRINT_JOB_FINISHED;

        this.onUserMessage(message, true);

        if (this.showPrintInActionDlg) {
            PrintInActionDialog.showMessage(message, this.webAppUriPrintIn,
                    this.resolvePrintInActionButton());
        }
    }

    @Override
    public void onPrintInExpired(final String message) {

        this.onUserMessage(message);

        if (this.showPrintInActionDlg) {
            PrintInActionDialog.showMessage(message, this.webAppUriPrintIn,
                    this.resolvePrintInActionButton());
        }
    }

    /**
     * Obtains image from path.
     *
     * @param path
     *            The path relative to the current class.
     * @return The {@link Image}.
     */
    protected static Image createImage(final String path) {

        final URL imageURL = ClientApp.class.getResource(path);

        if (imageURL == null) {
            throw new ClientAppException("Resource not found: " + path);
        }

        final String description = "the application icon";
        return (new ImageIcon(imageURL, description)).getImage();
    }

    /**
     * Displays a GUI error message.
     *
     * @param msg
     *            The message.
     */
    private static void showErrorMessage(final String msg) {
        JOptionPane.showMessageDialog(null, msg, getAppVersion(),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Sends a message by system command {@code notify-send}.
     *
     * @param msg
     *            The message.
     */
    private static void sysNotifySend(final String msg) {

        final ProcessBuilder pb =
                new ProcessBuilder("notify-send", MSG_BOX_TITLE, msg);
        try {
            pb.start();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * Main entry point.
     *
     * @param args
     *            The command-line arguments.
     */
    public static void main(final String[] args) {

        final ClientApp app = new ClientApp();

        try {

            final int status = app.run(args);

            if (status == EXIT_CODE_SHOW_HELP) {
                System.exit(EXIT_CODE_OK);
            }

            if (status != EXIT_CODE_OK) {
                System.exit(status);
            }

        } catch (Exception e) {
            showErrorMessage(String.format("%s: %s",
                    e.getClass().getSimpleName(), e.getMessage()));
            System.exit(EXIT_CODE_EXCEPTION);
        }

        // Do NOT call System.exit(status) here, because this immediately closes
        // the Swing application.
    }

}
