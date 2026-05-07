/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: (c) 2020 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class ProxyPrinterDto extends AbstractDto {

    /**
     * primary key.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * unique printer name.
     */
    @JsonProperty("printerName")
    private String printerName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("location")
    private String location;

    /**
     * A " ,;:" separated list of printer group names.
     */
    @JsonProperty("printerGroups")
    private String printerGroups;

    /**
     * A file with custom setting for PPD to IPP conversion and constraints.
     */
    @JsonProperty("ppdExtFile")
    private String ppdExtFile;

    /**
     * Local PPD file to create local PCL to send to CUPS Raw Printer Queue.
     */
    @JsonProperty("ppdFile")
    private String ppdFile;

    /**
     * File with rules to transform locally generated PCL before sending it to
     * CUPS Raw Printer Queue.
     */
    @JsonProperty("pdlTransformFile")
    private String pdlTransformFile;

    @JsonProperty("internal")
    private Boolean internal;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("archiveDisabled")
    private Boolean archiveDisabled;

    @JsonProperty("journalDisabled")
    private Boolean journalDisabled;

    @JsonProperty("papercutFrontEnd")
    private Boolean papercutFrontEnd;

    /**
     * Is (logically) deleted?
     */
    @JsonProperty("deleted")
    private Boolean deleted;

    /**
     * Is Job Ticket Printer?
     */
    @JsonProperty("jobTicket")
    private Boolean jobTicket;

    /** */
    @JsonProperty("jobTicketGroup")
    private String jobTicketGroup;

    /** */
    @JsonProperty("jobTicketLabelsEnabled")
    private Boolean jobTicketLabelsEnabled;

    /**
     * Is present in CUPS?
     */
    @JsonProperty("present")
    private Boolean present;

    /**
     * Is CUPS Make/Model = Raw/Raw Queue?
     */
    @JsonProperty("rawPrinter")
    private Boolean rawPrinter;

    /**
     * Is Local Raw Printing enabled?
     */
    @JsonProperty("rawPrintingEnabled")
    private Boolean rawPrintingEnabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPrinterGroups() {
        return printerGroups;
    }

    public void setPrinterGroups(String printerGroups) {
        this.printerGroups = printerGroups;
    }

    public String getPpdExtFile() {
        return ppdExtFile;
    }

    public void setPpdExtFile(String ppdExtFile) {
        this.ppdExtFile = ppdExtFile;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getArchiveDisabled() {
        return archiveDisabled;
    }

    public void setArchiveDisabled(Boolean archiveDisabled) {
        this.archiveDisabled = archiveDisabled;
    }

    public Boolean getJournalDisabled() {
        return journalDisabled;
    }

    public void setJournalDisabled(Boolean journalDisabled) {
        this.journalDisabled = journalDisabled;
    }

    public Boolean getPapercutFrontEnd() {
        return papercutFrontEnd;
    }

    public void setPapercutFrontEnd(Boolean papercutFrontEnd) {
        this.papercutFrontEnd = papercutFrontEnd;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public Boolean getJobTicket() {
        return jobTicket;
    }

    public void setJobTicket(Boolean jobTicket) {
        this.jobTicket = jobTicket;
    }

    public String getJobTicketGroup() {
        return jobTicketGroup;
    }

    public void setJobTicketGroup(String jobTicketGroup) {
        this.jobTicketGroup = jobTicketGroup;
    }

    public Boolean getJobTicketLabelsEnabled() {
        return jobTicketLabelsEnabled;
    }

    public void setJobTicketLabelsEnabled(Boolean jobTicketLabelsEnabled) {
        this.jobTicketLabelsEnabled = jobTicketLabelsEnabled;
    }

    public Boolean getRawPrinter() {
        return rawPrinter;
    }

    public void setRawPrinter(Boolean rawPrinter) {
        this.rawPrinter = rawPrinter;
    }

    public Boolean getRawPrintingEnabled() {
        return rawPrintingEnabled;
    }

    public void setRawPrintingEnabled(Boolean rawPrintingEnabled) {
        this.rawPrintingEnabled = rawPrintingEnabled;
    }

    public String getPpdFile() {
        return ppdFile;
    }

    public void setPpdFile(String ppdFile) {
        this.ppdFile = ppdFile;
    }

    public String getPdlTransformFile() {
        return pdlTransformFile;
    }

    public void setPdlTransformFile(String pdlTransformFile) {
        this.pdlTransformFile = pdlTransformFile;
    }

}
