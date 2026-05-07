/*
 * This file is part of the PrintFlowLite project <https://www.printflowlite.org>.
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

/**
 * @file printflowlite-notifier.cc
 *
 * @brief Notifier for the Common UNIX Printing System (CUPS).
 */

#include <cups/cups.h>

#include <errno.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/wait.h>

#include <time.h>

#include <XmlRpc.h>

/**
 * See XmlRpcServlet.properties in printflowlite-server repository.
 */
#define XMLRPC_METHOD_PFX "cups-event."

/*
 * Global variables
 */
static struct {
    std::string notifier;
    std::string host;
    short port;
} conf;

static const std::string API_ID = "printflowlite-backend-datraverse";
static const std::string API_KEY = "302c02147be5d6a3d2887a9ae650836855ed12a0bf8"
                                   "d88fc02144b7e6588d7d9b74326bf7d9e29082833d5"
                                   "4c8c94";

static const std::string mINFO = "INFO";
static const std::string mERROR = "ERROR";
static const std::string mDEBUG = "DEBUG";

typedef struct _JobEvent {
    std::string event;
    int job_id;
    int job_state;
    std::string job_name;
    std::string printer_name;
    int printer_state;
    int creation_time;
    int completed_time;
} JobEvent;

typedef struct _PrinterEvent {
    std::string event;
    std::string printer_name;
    int printer_state;
} PrinterEvent;

/*
 * CONSTANTS.
 */
static std::string glProgramVersion("1.2.0");

/*
 * Globals
 */
const char *glProgramName;

/*
 * Local functions...
 */
static void msg2cups(const std::string &prefix, const char *fmt, ...);
static void parseNotificationUri(const char *const pszUri);
static void print_attributes(ipp_t *ipp, int indent);
static void parseIppNotification(ipp_t *ipp);

int notifyEvent(const char *method, XmlRpc::XmlRpcValue &args);
int notifyPrinterEvent(PrinterEvent *notification);
int notifyJobEvent(JobEvent *notification);
void completePrintJobData(JobEvent *notification);
bool isPrintFlowLitePrinter(const std::string &printerName);

/**
 *
 */
void parseIppNotification(ipp_t *ipp) {

    int i;                 /* Looping var */
    ipp_tag_t group;       /* Current group */
    ipp_attribute_t *attr; /* Current attribute */
    ipp_tag_t groupNxt;    /* Next group */
    const char *attrNameWlk;

    JobEvent printJobEvent;
    PrinterEvent printerEvent;

    bool isJobEvent = false;
    bool isPrinterEvent = false;
    bool isCupsServerEvent = false;

    const char *event = NULL;

    /*
     * Looping over attributes.
     */
    for (group = IPP_TAG_ZERO, attr = ippFirstAttribute(ipp); attr != NULL;
         attr = ippNextAttribute(ipp)) {

        groupNxt = ippGetGroupTag(attr);

        /*
         * Skip zero group and attributes without a name.
         */
        if (groupNxt == IPP_TAG_ZERO || ippGetName(attr) == NULL) {
            group = IPP_TAG_ZERO;
            continue;
        }

        /*
         * Level break on group.
         */
        if (group != groupNxt) {
            group = groupNxt;
        }

        if (group != IPP_TAG_EVENT_NOTIFICATION) {
            continue;
        }

        /*
         * First value ...
         */
        attrNameWlk = ippGetName(attr);

        /*
         * notify-subscribed-event (keyword)
         */
        if (!strcmp("notify-subscribed-event", attrNameWlk)) {
            event = ippGetString(attr, 0, NULL);
            /*
             * CUPS Book p.193
             */
            if (!strcmp("job-completed", event) ||
                !strcmp("job-config-changed", event) ||
                !strcmp("job-created", event) ||
                !strcmp("job-progress", event) ||
                !strcmp("job-state-changed", event) ||
                !strcmp("job-stopped", event)) {

                // IPP events

                isJobEvent = true;

            } else if (!strcmp("printer-config-changed", event) ||
                       !strcmp("printer-finishings-changed", event) ||
                       !strcmp("printer-media-changed", event) ||
                       !strcmp("printer-queue-order-changed", event) ||
                       !strcmp("printer-restarted", event) ||
                       !strcmp("printer-shutdown", event) ||
                       !strcmp("printer-state-changed", event) ||
                       !strcmp("printer-stopped", event)) {

                // IPP events
                isPrinterEvent = true;

            } else if (!strcmp("printer-added", event) ||
                       !strcmp("printer-deleted", event) ||
                       !strcmp("printer-modified", event)) {
                // CUPS events
                isPrinterEvent = true;

            } else if (!strcmp("server-audit", event) ||
                       !strcmp("server-restarted", event) ||
                       !strcmp("server-started", event) ||
                       !strcmp("server-stopped", event)) {
                // CUPS events
                isCupsServerEvent = true;
            }
        }

        /*
         * notify-job-id (integer)
         */
        if (!strcmp("notify-job-id", attrNameWlk)) {
            printJobEvent.job_id = ippGetInteger(attr, 0);
        }

        /*
         * job-state (enum)
         */
        if (!strcmp("job-state", attrNameWlk)) {
            printJobEvent.job_state = ippGetInteger(attr, 0);
        }

        /*
         * job-name (nameWithoutLanguage)
         */
        if (!strcmp("job-name", attrNameWlk)) {
            printJobEvent.job_name = ippGetString(attr, 0, NULL);
        }

        /*
         * printer-name (nameWithoutLanguage)
         *
         * IMPORTANT: a 'job-completed' event with job status 'canceled'
         * does NOT notify the 'printer-name'. This is corrected in the
         * completePrintJobData() method where the 'printer_name' is set
         * with the 'dest' as found in the matching job object.
         *
         */
        if (!strcmp("printer-name", attrNameWlk)) {
            printJobEvent.printer_name = ippGetString(attr, 0, NULL);
            printerEvent.printer_name = ippGetString(attr, 0, NULL);
        }

        /*
         * printer-state (enum)
         */
        if (!strcmp("printer-state", attrNameWlk)) {
            printJobEvent.printer_state = ippGetInteger(attr, 0);
            printerEvent.printer_state = ippGetInteger(attr, 0);
        }
    }

    if (isJobEvent) {

        if (!isPrintFlowLitePrinter(printJobEvent.printer_name)) {
            printJobEvent.event = event;
            completePrintJobData(&printJobEvent);
            notifyJobEvent(&printJobEvent);
        }

    } else if (isPrinterEvent) {

        if (!isPrintFlowLitePrinter(printJobEvent.printer_name)) {
            printerEvent.event = event;
            notifyPrinterEvent(&printerEvent);
        }

    } else if (isCupsServerEvent) {

        XmlRpc::XmlRpcValue args;

        args[0] = API_ID;
        args[1] = API_KEY;
        args[2] = event;

        notifyEvent(XMLRPC_METHOD_PFX "serverEvent", args);
    }
}

/**
 * Completes the job data by reading the CUPS job log.
 */
void completePrintJobData(JobEvent *notification) {

    int i;
    cups_job_t *jobs;

    int num_jobs = cupsGetJobs(&jobs, notification->printer_name.c_str(), 0,
                               CUPS_WHICHJOBS_ALL);

    notification->creation_time = 0;
    notification->completed_time = 0;

    /*
     * Start at the back (most recent job).
     */
    for (i = num_jobs - 1; i >= 0; i--) {

        if (jobs[i].id == notification->job_id) {

            /*
             * Q: the user is 'unknown', why?
             *
             * A: As for the "withheld" in the web interface, that is new
             * security mojo in CUPS 1.5.0 that is controlled by the
             * JobPrivateAccess and JobPrivateValues directives in each
             * operation policy. Basically we now, by default, hide all
             * "personal" information in jobs unless you are an admin or the
             * owner of the job. You can disable this pretty easily to get
             * the pre-1.5.0 behavior by replacing the existing lines in
             * cupsd.conf with:
             *
             *  JobPrivateAccess all
             *  JobPrivateValues none
             *
             * and then restarting cupsd (editing via the web interface will
             * do this for you...)
             *
             * See:
             * http://comments.gmane.org/gmane.comp.printing.cups.general/28645
             */

            /*
             * NOTE: when executing cupsGetJobs as user 'rijk' (and when 'rijk'
             * is a member of the lpadmin group) the cupsGetJobs WILL return
             * the user of the print job.
             */

            /*
             * IMPORTANT: we set the 'printer_name' with the 'dest' as found
             * in the job. Reason: a 'job-completed' event with job status
             * 'canceled' does NOT notify the 'printer-name'.
             */
            notification->printer_name = jobs[i].dest;
            /*
             *
             */
            notification->creation_time = jobs[i].creation_time;
            notification->completed_time = jobs[i].completed_time;
            break;
        }
    }

    /* Free the job array */
    cupsFreeJobs(num_jobs, jobs);
}

/*
 * 'print_attributes()' - Print the attributes in a request...
 */
void print_attributes(ipp_t *ipp, /* I - IPP request */
                      int indent) /* I - Indentation */
{

//#define MSG_PFX "ERROR"
#define MSG_PFX "DEBUG"

    int i;                 /* Looping var */
    ipp_tag_t group;       /* Current group */
    ipp_attribute_t *attr; /* Current attribute */
    ipp_tag_t groupNxt;    /* Next group */
    const char *attrNameWlk;
    ipp_tag_t valueTag;
    int count;

    static const char *const tags[] = /* Value/group tag strings */
        {"reserved-00",
         "operation-attributes-tag",
         "job-attributes-tag",
         "end-of-attributes-tag",
         "printer-attributes-tag",
         "unsupported-attributes-tag",
         "subscription-attributes-tag",
         "event-attributes-tag",
         "reserved-08",
         "reserved-09",
         "reserved-0A",
         "reserved-0B",
         "reserved-0C",
         "reserved-0D",
         "reserved-0E",
         "reserved-0F",
         "unsupported",
         "default",
         "unknown",
         "no-value",
         "reserved-14",
         "not-settable",
         "delete-attr",
         "admin-define",
         "reserved-18",
         "reserved-19",
         "reserved-1A",
         "reserved-1B",
         "reserved-1C",
         "reserved-1D",
         "reserved-1E",
         "reserved-1F",
         "reserved-20",
         "integer",
         "boolean",
         "enum",
         "reserved-24",
         "reserved-25",
         "reserved-26",
         "reserved-27",
         "reserved-28",
         "reserved-29",
         "reserved-2a",
         "reserved-2b",
         "reserved-2c",
         "reserved-2d",
         "reserved-2e",
         "reserved-2f",
         "octetString",
         "dateTime",
         "resolution",
         "rangeOfInteger",
         "begCollection",
         "textWithLanguage",
         "nameWithLanguage",
         "endCollection",
         "reserved-38",
         "reserved-39",
         "reserved-3a",
         "reserved-3b",
         "reserved-3c",
         "reserved-3d",
         "reserved-3e",
         "reserved-3f",
         "reserved-40",
         "textWithoutLanguage",
         "nameWithoutLanguage",
         "reserved-43",
         "keyword",
         "uri",
         "uriScheme",
         "charset",
         "naturalLanguage",
         "mimeMediaType",
         "memberName"};

    for (group = IPP_TAG_ZERO, attr = ippFirstAttribute(ipp); attr != NULL;
         attr = ippNextAttribute(ipp)) {

        attrNameWlk = ippGetName(attr);
        groupNxt = ippGetGroupTag(attr);

        if ((groupNxt == IPP_TAG_ZERO && indent <= 8) || attrNameWlk == NULL) {
            group = IPP_TAG_ZERO;
            fputc('\n', stderr);
            continue;
        }

        if (group != groupNxt) {
            group = groupNxt;

            fprintf(stderr, MSG_PFX ": %*s%s:\n\n", indent - 4, "",
                    tags[group]);
        }

        fprintf(stderr, MSG_PFX ": %*s%s (", indent, "", attrNameWlk);

        if (ippGetCount(attr) > 1) {
            fputs("1setOf ", stderr);
        }

        valueTag = ippGetValueTag(attr);
        fprintf(stderr, "%s):", tags[valueTag]);

        switch (valueTag) {
        case IPP_TAG_ENUM:
        case IPP_TAG_INTEGER:
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                fprintf(stderr, " %d", ippGetInteger(attr, i));
            }
            fputc('\n', stderr);
            break;

        case IPP_TAG_BOOLEAN:
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                fprintf(stderr, " %s",
                        ippGetBoolean(attr, i) ? "true" : "false");
            }
            fputc('\n', stderr);
            break;

        case IPP_TAG_RANGE:
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                int upper;
                int lower = ippGetRange(attr, i, &upper);
                fprintf(stderr, " %d-%d", lower, upper);
            }
            fputc('\n', stderr);
            break;

        case IPP_TAG_DATE: {
            time_t vtime;      /* Date/Time value */
            struct tm *vdate;  /* Date info */
            char vstring[256]; /* Formatted time */

            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                vtime = ippDateToTime(ippGetDate(attr, i));
                vdate = localtime(&vtime);
                strftime(vstring, sizeof(vstring), "%c", vdate);
                fprintf(stderr, " (%s)", vstring);
            }
        }
            fputc('\n', stderr);
            break;

        case IPP_TAG_RESOLUTION:
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                ipp_res_t units;
                int yres;
                int xres = ippGetResolution(attr, i, &yres, &units);
                fprintf(stderr, " %dx%d%s", xres, yres,
                        units == IPP_RES_PER_INCH ? "dpi" : "dpc");
            }
            fputc('\n', stderr);
            break;

        case IPP_TAG_STRING:
        case IPP_TAG_TEXTLANG:
        case IPP_TAG_NAMELANG:
        case IPP_TAG_TEXT:
        case IPP_TAG_NAME:
        case IPP_TAG_KEYWORD:
        case IPP_TAG_URI:
        case IPP_TAG_URISCHEME:
        case IPP_TAG_CHARSET:
        case IPP_TAG_LANGUAGE:
        case IPP_TAG_MIMETYPE:
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                fprintf(stderr, " \"%s\"", ippGetString(attr, i, NULL));
            }
            fputc('\n', stderr);
            break;

        case IPP_TAG_BEGIN_COLLECTION:
            fputc('\n', stderr);
            for (i = 0, count = ippGetCount(attr); i < count; i++) {
                if (i) {
                    fputc('\n', stderr);
                }
                print_attributes(ippGetCollection(attr, i), indent + 4);
            }
            break;

        default:
            fprintf(stderr, "UNKNOWN (%d values)\n", ippGetCount(attr));
            break;
        }
    }
}

/**
 * Parses the host and port of the server from the notifier URI
 */
void parseNotificationUri(const char *const pszUri) {

    enum ParseState { P_START, P_SLASHES, P_HOST, P_PORT, P_END };

    ParseState state = P_START;
    std::string token = "";

    // Sample: printflowlite:host:port

    for (const char *pWlk = pszUri; *pWlk && state != P_END; pWlk++) {
        switch (state) {
        case P_START:
            if (*pWlk == ':') {
                state = P_SLASHES;
            }
            break;
        case P_SLASHES:
            if (*pWlk != '/') {
                state = P_HOST;
                token = *pWlk;
            }
            break;
        case P_HOST:
            if (*pWlk == ':') {
                conf.host = token;
                token = "";
                state = P_PORT;
            } else {
                token += *pWlk;
            }
            break;
        case P_PORT:
            if (*pWlk == '/') {
                conf.port = atoi(token.c_str());
                state = P_END;
            } else {
                token += *pWlk;
            }
            break;
        }
    }

    if (P_HOST == state && token.length() > 0) {
        conf.host = token;
    }
    if (P_PORT == state && token.length() > 0) {
        conf.port = atoi(token.c_str());
    }
}

/**
 * Sends a message to the CUPS job scheduler.
 */
static void msg2cups(const std::string &prefix, const char *fmt, ...) {

    fprintf(stderr, "%s: %s: ", prefix.c_str(), conf.notifier.c_str());

    va_list vl;
    va_start(vl, fmt);
    vfprintf(stderr, fmt, vl);
    va_end(vl);
    fprintf(stderr, "\n");

    /*
     Communicating with the Scheduler

     Filters and backends communicate with the scheduler by writing messages to
     the standard error file. The scheduler reads messages from all filters in a
     job and processes the message based on its prefix. For example, the
     following
     code sets the current printer state message to "Printing page 5":

     {
     int page = 5;

     fprintf(stderr, "INFO: Printing page %d\n", page);
     }

     Each message is a single line of text starting with one of the following
     prefix strings:

     ALERT: message
     Sets the printer-state-message attribute and adds the specified message to
     the current error log file using the "alert" log level.

     ATTR: attribute=value [attribute=value]

     Sets the named printer or job attribute(s). Typically this is used to set
     the marker-colors, marker-high-levels, marker-levels, marker-low-levels,
     marker-message, marker-names, marker-types, printer-alert, and
     printer-alert-description printer attributes. Standard marker-types
     values are listed in ... (table below)

     marker-type        Description
     -----------------  --------------------
     developer          Developer unit
     fuser              Fuser unit
     fuserCleaningPad   Fuser cleaning pad
     fuserOil           Fuser oil
     ink                Ink supply
     opc                Photo conductor
     solidWax           Wax supply
     staples            Staple supply
     toner              Toner supply
     transferUnit       Transfer unit
     wasteInk           Waste ink tank
     wasteToner         Waste toner tank
     wasteWax           Waste wax tank

     CRIT: message
     Sets the printer-state-message attribute and adds the specified message
     to the current error log file using the "critical" log level.

     DEBUG: message
     Sets the printer-state-message attribute and adds the specified message
     to the current error log file using the "debug" log level.

     DEBUG2: message
     Sets the printer-state-message attribute and adds the specified message
     to the current error log file using the "debug2" log level.

     EMERG: message
     Sets the printer-state-message attribute and adds the specified message
     to the current error log file using the "emergency" log level.

     ERROR: message
     Sets the printer-state-message attribute and adds the specified message
     to the current error log file using the "error" log level.
     Use "ERROR:" messages for non-persistent processing errors.

     INFO: message
     Sets the printer-state-message attribute. If the current log level is set
     to "debug2", also adds the specified message to the current error log file
     using the "info" log level.

     NOTICE: message
     Sets the printer-state-message attribute and adds the specified message to
     the current error log file using the "notice" log level.

     PAGE: page-number #-copies
     PAGE: total #-pages

     Adds an entry to the current page log file.

     The first form adds #-copies to the job-media-sheets-completed attribute.

     The second form sets the job-media-sheets-completed attribute to #-pages.

     PPD: keyword=value [keyword=value ...]
     Changes or adds keywords to the printer's PPD file. Typically this is used
     to update installable options or default media settings based on the
     printer
     configuration.

     STATE: + printer-state-reason [printer-state-reason ...]
     STATE: - printer-state-reason [printer-state-reason ...]

     Sets or clears printer-state-reason keywords for the current queue.
     Typically this is used to indicate persistent media, ink, toner, and
     configuration conditions or errors on a printer.

     Table 2 (below) lists the standard state keywords

     - use vendor-prefixed ("com.example.foo") keywords for custom states.
     See Managing Printer State in a Filter for more information.

     Keyword                    Description
     -------------------------  -----------------------------------------------
     connecting-to-device       Connecting to printer but not printing yet.
     cover-open                 The printer's cover is open.
     input-tray-missing         The paper tray is missing.
     marker-supply-empty        The printer is out of ink.
     marker-supply-low          The printer is almost out of ink.
     marker-waste-almost-full   The printer's waste bin is almost full.
     marker-waste-full          The printer's waste bin is full.
     media-empty                The paper tray (any paper tray) is empty.
     media-jam                  There is a paper jam.
     media-low                  The paper tray (any paper tray) is almost empty.
     media-needed               The paper tray needs to be filled (for a job
     that is printing).
     paused                     Stop the printer.
     timed-out                  Unable to connect to printer.
     toner-empty                The printer is out of toner.
     toner-low                  The printer is low on toner.

     WARNING: message
     Sets the printer-state-message attribute and adds the specified message to
     the current error log file using the "warning" log level.

     Messages without one of these prefixes are treated as if they began with
     the "DEBUG:" prefix string.
     */
}

/**
 *
 */
int notifyPrinterEvent(PrinterEvent *event) {

    XmlRpc::XmlRpcValue args;

    args[0] = API_ID;
    args[1] = API_KEY;
    args[2] = event->event;
    args[3] = event->printer_name;
    args[4] = event->printer_state;

    return notifyEvent(XMLRPC_METHOD_PFX "printerEvent", args);
}

/**
 *
 */
int notifyJobEvent(JobEvent *notification) {

    XmlRpc::XmlRpcValue args;

    args[0] = API_ID;
    args[1] = API_KEY;
    args[2] = notification->event;
    args[3] = notification->job_id;
    args[4] = notification->job_name;
    args[5] = notification->job_state;
    args[6] = notification->creation_time;
    args[7] = notification->completed_time;
    args[8] = notification->printer_name;
    args[9] = notification->printer_state;

    return notifyEvent(XMLRPC_METHOD_PFX "jobEvent", args);
}

/**
 *
 */
int notifyEvent(const char *method, XmlRpc::XmlRpcValue &args) {

    int rc = 1;
    using namespace XmlRpc;

    std::string reason;

    try {
        // Create a client and connect to the server at hostname:port
        XmlRpcClient client(static_cast<const char *>(conf.host.c_str()),
                            conf.port, "/xmlrpc");

        XmlRpcValue result;

        if (client.execute(method, args, result)) {

            if (client.isFault()) {
                reason = "fault response";
            } else {
                if (static_cast<int>(result["rc"]) == 0) {
                    rc = 0;
                } else {
                    reason = static_cast<std::string>(result["error"]);
                }
            }

        } else {
            reason = "no connection";
        }

        // Mantis #489.
        client.close();

    } catch (XmlRpcException &ex) {
        reason = ex.getMessage();
    } catch (...) {
        reason = "unknown exception";
    }

    if (1 == rc) {
        msg2cups(mERROR, "call to %s:%i failed: %s", conf.host.c_str(),
                 conf.port, reason.c_str());
    }
    return rc;
}

/**
 *
 */
std::string getPrinterMakeModel(const char *printerName) {

    cups_dest_t *dests;
    int num_dests = cupsGetDests(&dests);
    cups_dest_t *dest = cupsGetDest(printerName, NULL, num_dests, dests);

    std::string ret;

    if (dest) {
        ret = cupsGetOption("printer-make-and-model", dest->num_options,
                            dest->options);
    }

    cupsFreeDests(num_dests, dests);
    return ret;
}

/**
 *
 */
void stoupper(std::string &s) {

    std::string::iterator i = s.begin();
    std::string::iterator end = s.end();

    while (i != end) {
        *i = std::toupper((unsigned char)*i);
        ++i;
    }
}

/**
 * Tells if printerName is a PrintFlowLite printer (by checking Make and Model of
 * printerName).
 */
bool isPrintFlowLitePrinter(const std::string &printerName) {

    std::string model = getPrinterMakeModel(printerName.c_str());
    stoupper(model);
    return (!strncmp(model.c_str(), "PRINTFLOWLITE", 8));
}

/**
 * Parses the program name from the command-line invocation.
 */
const char *parseProgramName(const char *programPath) {
    const char *name;
    for (name = programPath + strlen(programPath);
         name != programPath && *name != '/'; name--)
        ;
    if (*name == '/') {
        name++;
    }
    return name;
}

/*
 * Main entry for the printflowlite notifier.
 */
int main(int argc, char *argv[]) {

    int i;             /* Looping var */
    ipp_t *msg;        /* Event message from scheduler */
    ipp_state_t state; /* IPP event state */
    char *subject,     /* Subject for notification message */
        *text;         /* Text for notification message */
    cups_lang_t *lang; /* Language info */
    char temp[1024];   /* Temporary string */
    int templen;       /* Length of temporary string */

#if defined(HAVE_SIGACTION) && !defined(HAVE_SIGSET)
    struct sigaction action; /* POSIX sigaction data */
#endif                       /* HAVE_SIGACTION && !HAVE_SIGSET */

#ifdef PRODUCT_VERSION
    glProgramVersion = PRODUCT_VERSION;
#endif

    glProgramName = parseProgramName(argv[0]);

    /*
     * Don't buffer stderr...
     */

    setbuf(stderr, NULL);

/*
 * Ignore SIGPIPE signals...
 */

#ifdef HAVE_SIGSET
    sigset(SIGPIPE, SIG_IGN);
#elif defined(HAVE_SIGACTION)
    memset(&action, 0, sizeof(action));
    action.sa_handler = SIG_IGN;
    sigaction(SIGPIPE, &action, NULL);
#else
    signal(SIGPIPE, SIG_IGN);
#endif /* HAVE_SIGSET */

    /*
     * Validate command-line options...
     */
    conf.notifier = basename(argv[0]);

    if (argc != 3) {
        printf(
            "__________________________________________________________________"
            "__________\n"
            "PrintFlowLite CUPS Notifier v%s\n"
            "(c) 2020, Datraverse B.V.\n"
            "\n"
            "License: GNU AGPL version 3 or later "
            "<https://www.gnu.org/licenses/agpl.html>.\n"
            "         This is free software: you are free to change and "
            "redistribute it.\n"
            "         There is NO WARRANTY, to the extent permitted by law.\n"
            "\n"
            "Usage: %s printflowlite:host:port notify-user-data\n\n",
            glProgramVersion.c_str(), glProgramName);
        return (1);
    }

    if (strncmp(argv[1], "printflowlite", 8)) {
        fprintf(stderr, "ERROR: Bad recipient \"%s\"!\n", argv[1]);
        return (1);
    }

    fprintf(stderr, "DEBUG: argc=%d\n", argc);
    for (i = 0; i < argc; i++) {
        fprintf(stderr, "DEBUG: argv[%d]=\"%s\"\n", i, argv[i]);
    }

    parseNotificationUri(argv[1]);

    fprintf(stderr, "DEBUG: printflowlite-notifier [%s]\n", argv[1]);

    /*
     * Load configuration data...
     */

    if ((lang = cupsLangDefault()) == NULL)
        return (1);

    /*
     * Loop forever until we run out of events...
     */
    for (;;) {

        /*
         * Get the next event...
         */

        msg = ippNew();

        while ((state = ippReadFile(0, msg)) != IPP_DATA) {
            if (state <= IPP_IDLE)
                break;
        }

        fprintf(stderr, "DEBUG: state=%d\n", state);

        if (state == IPP_ERROR)
            fputs("DEBUG: ippReadFile() returned IPP_ERROR!\n", stderr);

        if (state <= IPP_IDLE) {
            /*
             * Out of messages, free memory and then exit...
             */
            ippDelete(msg);
            return (0);
        }

        /*
         * Get the subject and text for the message, then ...
         */
        subject = cupsNotifySubject(lang, msg);
        text = cupsNotifyText(lang, msg);

        parseIppNotification(msg);

        // print_attributes(msg, 4); // TEST

        /*
         *
         */
        if (subject && text) {

            // email_message(argv[1] + 7, subject, text);

        } else {
            fputs("ERROR: Missing attributes in event notification!\n", stderr);
            print_attributes(msg, 4);
        }

        /*
         * Free the memory used for this event...
         */
        if (subject) {
            free(subject);
        }

        if (text) {
            free(text);
        }

        ippDelete(msg);
    }
}

/* end-of-file */
