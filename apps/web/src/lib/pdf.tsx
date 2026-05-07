// PDF generation utilities for the frontend
// Uses react-pdf (@react-pdf/renderer) for PDF generation on the client side

import { Document, Page, Text, View, StyleSheet, pdf, Font, type PDFDocument } from "@react-pdf/renderer";
import React from "react";

// ─── Styles ───────────────────────────────────────────────────────────────────

export const pdfStyles = StyleSheet.create({
  page: { padding: 40, fontFamily: "Helvetica" },
  header: { fontSize: 20, marginBottom: 20, textAlign: "center", color: "#2563eb" },
  subHeader: { fontSize: 14, marginBottom: 10, color: "#374151" },
  section: { marginBottom: 15 },
  row: { flexDirection: "row", marginBottom: 5 },
  label: { fontSize: 10, color: "#6b7280", width: "40%" },
  value: { fontSize: 10, color: "#111827", width: "60%" },
  bold: { fontFamily: "Helvetica-Bold" },
  table: { borderWidth: 1, borderColor: "#e5e7eb", marginBottom: 10 },
  tableHeader: { flexDirection: "row", backgroundColor: "#f3f4f6", padding: 5 },
  tableRow: { flexDirection: "row", borderTopWidth: 1, borderColor: "#e5e7eb", padding: 5 },
  tableCell: { fontSize: 9, flex: 1 },
  footer: { position: "absolute", bottom: 30, left: 40, right: 40, textAlign: "center", fontSize: 8, color: "#9ca3af" },
  divider: { borderBottomWidth: 1, borderColor: "#e5e7eb", marginVertical: 10 },
  badge: { paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 },
  badgeGreen: { backgroundColor: "#d1fae5" },
  badgeYellow: { backgroundColor: "#fef3c7" },
  badgeRed: { backgroundColor: "#fee2e2" },
  badgeBlue: { backgroundColor: "#dbeafe" },
});

// ─── Document Components ─────────────────────────────────────────────────────────

interface JobTicketPdfProps {
  ticketNumber: string;
  userName: string;
  operatorName: string;
  printerName: string;
  copies: number;
  docName: string;
  submitTime: string;
  deliveryTime: string;
  totalCost: string;
}

export function JobTicketPdfDocument({ ticketNumber, userName, operatorName, printerName, copies, docName, submitTime, deliveryTime, totalCost }: JobTicketPdfProps) {
  return (
    <Document>
      <Page size="A5" style={pdfStyles.page}>
        <Text style={pdfStyles.header}>PRINTFLOW — JOB TICKET</Text>
        <View style={pdfStyles.divider} />

        <View style={pdfStyles.section}>
          <Text style={pdfStyles.subHeader}>Ticket Information</Text>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Ticket Number</Text>
            <Text style={[pdfStyles.value, pdfStyles.bold]}>{ticketNumber}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Document</Text>
            <Text style={pdfStyles.value}>{docName}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Copies</Text>
            <Text style={pdfStyles.value}>{copies}</Text>
          </View>
        </View>

        <View style={pdfStyles.section}>
          <Text style={pdfStyles.subHeader}>Print Details</Text>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Printer</Text>
            <Text style={pdfStyles.value}>{printerName}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>User</Text>
            <Text style={pdfStyles.value}>{userName}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Operator</Text>
            <Text style={pdfStyles.value}>{operatorName}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Total Cost</Text>
            <Text style={pdfStyles.value}>{totalCost}</Text>
          </View>
        </View>

        <View style={pdfStyles.section}>
          <Text style={pdfStyles.subHeader}>Schedule</Text>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Submitted</Text>
            <Text style={pdfStyles.value}>{new Date(submitTime).toLocaleString()}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Delivery Date</Text>
            <Text style={pdfStyles.value}>{new Date(deliveryTime).toLocaleString()}</Text>
          </View>
        </View>

        <View style={pdfStyles.divider} />
        <Text style={{ ...pdfStyles.footer, ...{ textAlign: "center" } }}>
          Present this ticket at the print station. PrintFlow Automated System.
        </Text>
      </Page>
    </Document>
  );
}

interface PrintJobReportPdfProps {
  title: string;
  generatedAt: string;
  generatedBy: string;
  jobs: Array<{
    id: number;
    docName: string;
    userName: string;
    printerName: string;
    copies: number;
    totalCost: string;
    status: string;
    dateCreated: string;
  }>;
  summary?: {
    totalJobs: number;
    totalPages: number;
    totalCost: string;
    completedCount: number;
    pendingCount: number;
    cancelledCount: number;
  };
}

export function PrintJobReportDocument({ title, generatedAt, generatedBy, jobs, summary }: PrintJobReportPdfProps) {
  return (
    <Document>
      <Page size="A4" style={pdfStyles.page}>
        <Text style={pdfStyles.header}>{title}</Text>
        <View style={{ marginBottom: 20 }}>
          <Text style={{ fontSize: 10, color: "#6b7280" }}>Generated: {generatedAt} by {generatedBy}</Text>
        </View>

        {summary && (
          <View style={pdfStyles.section}>
            <Text style={pdfStyles.subHeader}>Summary</Text>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Total Jobs</Text>
              <Text style={pdfStyles.value}>{summary.totalJobs}</Text>
            </View>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Total Pages</Text>
              <Text style={pdfStyles.value}>{summary.totalPages}</Text>
            </View>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Total Cost</Text>
              <Text style={pdfStyles.value}>{summary.totalCost}</Text>
            </View>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Completed</Text>
              <Text style={[pdfStyles.value, { color: "#059669" }]}>{summary.completedCount}</Text>
            </View>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Pending</Text>
              <Text style={[pdfStyles.value, { color: "#d97706" }]}>{summary.pendingCount}</Text>
            </View>
            <View style={pdfStyles.row}>
              <Text style={pdfStyles.label}>Cancelled</Text>
              <Text style={[pdfStyles.value, { color: "#dc2626" }]}>{summary.cancelledCount}</Text>
            </View>
          </View>
        )}

        <View style={pdfStyles.table}>
          <View style={pdfStyles.tableHeader}>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>ID</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Document</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>User</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Printer</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Copies</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Cost</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Status</Text>
          </View>
          {jobs.map((job, i) => (
            <View key={job.id} style={pdfStyles.tableRow}>
              <Text style={pdfStyles.tableCell}>{job.id}</Text>
              <Text style={pdfStyles.tableCell}>{job.docName?.slice(0, 20) ?? "—"}</Text>
              <Text style={pdfStyles.tableCell}>{job.userName}</Text>
              <Text style={pdfStyles.tableCell}>{job.printerName}</Text>
              <Text style={pdfStyles.tableCell}>{job.copies}</Text>
              <Text style={pdfStyles.tableCell}>{job.totalCost}</Text>
              <Text style={pdfStyles.tableCell}>{job.status}</Text>
            </View>
          ))}
        </View>

        <Text style={pdfStyles.footer}>
          PrintFlow Print Job Report — Confidential
        </Text>
      </Page>
    </Document>
  );
}

interface AccountStatementPdfProps {
  accountName: string;
  userName: string;
  period: string;
  generatedAt: string;
  balance: string;
  overdraft: string;
  transactions: Array<{
    date: string;
    description: string;
    amount: string;
    balance: string;
    reference: string;
  }>;
  totals: {
    totalIn: string;
    totalOut: string;
    netChange: string;
  };
}

export function AccountStatementDocument({ accountName, userName, period, generatedAt, balance, overdraft, transactions, totals }: AccountStatementPdfProps) {
  return (
    <Document>
      <Page size="A4" style={pdfStyles.page}>
        <Text style={pdfStyles.header}>PRINTFLOW — ACCOUNT STATEMENT</Text>

        <View style={{ marginBottom: 20 }}>
          <Text style={{ fontSize: 10, color: "#6b7280" }}>Account: {accountName}</Text>
          <Text style={{ fontSize: 10, color: "#6b7280" }}>User: {userName}</Text>
          <Text style={{ fontSize: 10, color: "#6b7280" }}>Period: {period}</Text>
          <Text style={{ fontSize: 10, color: "#6b7280" }}>Generated: {generatedAt}</Text>
        </View>

        <View style={pdfStyles.row}>
          <Text style={pdfStyles.label}>Current Balance</Text>
          <Text style={[pdfStyles.value, pdfStyles.bold]}>{balance}</Text>
        </View>
        <View style={pdfStyles.row}>
          <Text style={pdfStyles.label}>Overdraft Limit</Text>
          <Text style={pdfStyles.value}>{overdraft}</Text>
        </View>

        <View style={pdfStyles.divider} />

        <View style={pdfStyles.table}>
          <View style={pdfStyles.tableHeader}>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Date</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Description</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Amount</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Balance</Text>
            <Text style={[pdfStyles.tableCell, { fontFamily: "Helvetica-Bold" }]}>Reference</Text>
          </View>
          {transactions.map((trx, i) => (
            <View key={i} style={pdfStyles.tableRow}>
              <Text style={pdfStyles.tableCell}>{new Date(trx.date).toLocaleDateString()}</Text>
              <Text style={pdfStyles.tableCell}>{trx.description}</Text>
              <Text style={[pdfStyles.tableCell, { color: parseFloat(trx.amount) >= 0 ? "#059669" : "#dc2626" }]}>
                {parseFloat(trx.amount) >= 0 ? "+" : ""}{trx.amount}
              </Text>
              <Text style={pdfStyles.tableCell}>{trx.balance}</Text>
              <Text style={pdfStyles.tableCell}>{trx.reference}</Text>
            </View>
          ))}
        </View>

        <View style={{ marginTop: 20 }}>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Total Credits</Text>
            <Text style={[pdfStyles.value, { color: "#059669" }]}>+{totals.totalIn}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={pdfStyles.label}>Total Debits</Text>
            <Text style={[pdfStyles.value, { color: "#dc2626" }]}>{totals.totalOut}</Text>
          </View>
          <View style={pdfStyles.row}>
            <Text style={[pdfStyles.label, pdfStyles.bold]}>Net Change</Text>
            <Text style={[pdfStyles.value, pdfStyles.bold]}>{totals.netChange}</Text>
          </View>
        </View>

        <Text style={pdfStyles.footer}>
          PrintFlow Account Statement — Confidential
        </Text>
      </Page>
    </Document>
  );
}

// ─── PDF Generation Functions ────────────────────────────────────────────────────

export async function generateJobTicketPdf(props: JobTicketPdfProps): Promise<Blob> {
  const doc = <JobTicketPdfDocument {...props} />;
  return await pdf(doc).toBlob();
}

export async function generatePrintJobReportPdf(props: PrintJobReportPdfProps): Promise<Blob> {
  const doc = <PrintJobReportDocument {...props} />;
  return await pdf(doc).toBlob();
}

export async function generateAccountStatementPdf(props: AccountStatementPdfProps): Promise<Blob> {
  const doc = <AccountStatementDocument {...props} />;
  return await pdf(doc).toBlob();
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export async function generateAndDownloadJobTicket(props: JobTicketPdfProps) {
  const blob = await generateJobTicketPdf(props);
  downloadBlob(blob, `ticket-${props.ticketNumber}.pdf`);
}

export async function generateAndDownloadReport(props: PrintJobReportPdfProps) {
  const blob = await generatePrintJobReportPdf(props);
  const date = new Date().toISOString().slice(0, 10);
  downloadBlob(blob, `print-report-${date}.pdf`);
}

export async function generateAndDownloadStatement(props: AccountStatementPdfProps) {
  const blob = await generateAccountStatementPdf(props);
  const date = new Date().toISOString().slice(0, 10);
  downloadBlob(blob, `statement-${props.accountName}-${date}.pdf`);
}
