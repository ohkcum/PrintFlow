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
package org.printflow.lite.core.template.dto;

import java.util.Locale;

import org.printflow.lite.common.SystemPropertyEnum;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.config.ConfigManager;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateAppDto implements TemplateDto {

    private String name;
    private String nameVersion;
    private String nameVersionBuild;

    private String nameVersionJava;

    private String slogan;

    private String PrintFlowLiteDotOrg;
    private String wwwPrintFlowLiteDotOrgUrl;
    private String wwwPrintFlowLiteDotOrg;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameVersion() {
        return nameVersion;
    }

    public void setNameVersion(String nameVersion) {
        this.nameVersion = nameVersion;
    }

    public String getNameVersionBuild() {
        return nameVersionBuild;
    }

    public void setNameVersionBuild(String nameVersionBuild) {
        this.nameVersionBuild = nameVersionBuild;
    }

    public String getNameVersionJava() {
        return nameVersionJava;
    }

    public void setNameVersionJava(String nameVersionJava) {
        this.nameVersionJava = nameVersionJava;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
    }

    public String getPrintFlowLiteDotOrg() {
        return PrintFlowLiteDotOrg;
    }

    public void setPrintFlowLiteDotOrg(String PrintFlowLiteDotOrg) {
        this.PrintFlowLiteDotOrg = PrintFlowLiteDotOrg;
    }

    public String getWwwPrintFlowLiteDotOrgUrl() {
        return wwwPrintFlowLiteDotOrgUrl;
    }

    public void setWwwPrintFlowLiteDotOrgUrl(String wwwPrintFlowLiteDotOrgUrl) {
        this.wwwPrintFlowLiteDotOrgUrl = wwwPrintFlowLiteDotOrgUrl;
    }

    public String getWwwPrintFlowLiteDotOrg() {
        return wwwPrintFlowLiteDotOrg;
    }

    public void setWwwPrintFlowLiteDotOrg(String wwwPrintFlowLiteDotOrg) {
        this.wwwPrintFlowLiteDotOrg = wwwPrintFlowLiteDotOrg;
    }

    /**
     *
     * @param locale
     * @return Template.
     */
    public static TemplateAppDto create(final Locale locale) {

        final TemplateAppDto dto = new TemplateAppDto();

        dto.name = CommunityDictEnum.PrintFlowLite.getWord(locale);
        dto.nameVersion = ConfigManager.getAppNameVersion();
        dto.nameVersionBuild = ConfigManager.getAppNameVersionBuild();

        dto.nameVersionJava = SystemPropertyEnum.JAVA_VM_NAME.getValue() + " ("
                + SystemPropertyEnum.JAVA_VERSION.getValue() + ")";

        dto.wwwPrintFlowLiteDotOrgUrl =
                CommunityDictEnum.PRINTFLOWLITE_WWW_DOT_ORG_URL.getWord(locale);
        dto.PrintFlowLiteDotOrg = CommunityDictEnum.PRINTFLOWLITE_DOT_ORG.getWord(locale);
        dto.wwwPrintFlowLiteDotOrg =
                CommunityDictEnum.PRINTFLOWLITE_WWW_DOT_ORG.getWord(locale);

        dto.slogan = CommunityDictEnum.PRINTFLOWLITE_SLOGAN.getWord(locale);

        return dto;
    }

}
