# PrintFlowLite User Guide

This guide explains how to use the PrintFlowLite Web App as an end user.

---

## Accessing PrintFlowLite

### Web Browser

Open your browser and navigate to the PrintFlowLite server address provided by your administrator:

```
https://your-printflowlite-server:8632/
```

Log in with your username and password.

### Mobile Access

The PrintFlowLite Web App is mobile-responsive and works on any device with an HTML5 browser. No app installation required.

### Desktop Client (Optional)

The optional PrintFlowLite Desktop Client provides real-time notifications and quick access from your system tray. Download from the User Web App About section.

---

## Logging In

1. Navigate to the PrintFlowLite URL
2. Enter your username and password
3. Click **Login**
4. If your organization uses LDAP or OAuth, you may be redirected to an identity provider

> **Tip**: Bookmark the login page for quick access.

---

## The Main Page

After logging in, you see the main page with these sections:

- **Header** — Your username, balance (if tracked), and navigation
- **Navigation Tabs** — SafePages, Printers, User Details
- **Content Area** — Thumbnails of your print jobs
- **Footer** — Quick actions and links

---

## SafePages (Your Print Queue)

### Viewing Your Print Jobs

All documents you print to PrintFlowLite appear in your SafePages as thumbnail pages. You can:

- **Scroll** through pages of thumbnails
- **Zoom** in on any page for more detail
- **Select** pages for printing or deletion

### Deleting Pages

1. Select one or more pages by clicking thumbnails
2. Click the **Delete** button
3. Confirm deletion

This removes only the selected pages from the print job. The original document is not affected.

### Deleting Entire Jobs

1. Hover over a print job in the list
2. Click the **Delete** icon
3. Confirm deletion

### Page Range Selection

For multi-page documents, you can select a specific page range:

1. Click **Select Pages** or **Page Range**
2. Enter the page numbers (e.g., `1-5, 8, 10-12`)
3. Click **Apply**

---

## Applying Letterhead

A letterhead is a background image (e.g., your company's header) applied to all pages.

### Setting Your Letterhead

1. Go to **User Details**
2. In the **Letterhead** section, select a letterhead document
3. Click **Save**

### Previewing with Letterhead

Return to your SafePages to see the letterhead applied as a background.

### Letterhead Options

- **Standard** — Apply to all pages
- **First Page Only** — Apply only to the first page
- **None** — No letterhead

---

## Proxy Printing

Proxy printing is the process of selecting a physical printer to print your SafePages.

### Selecting a Proxy Printer

1. From your SafePages, select the pages you want to print
2. Click the **Print** button
3. A list of available Proxy Printers appears
4. Select your desired printer

> Proxy Printers are physical devices managed by PrintFlowLite. You do not need their driver installed.

### Configuring Print Options

After selecting a printer, you can configure:

| Option | Description |
|---|---|
| **Copies** | Number of copies |
| **Duplex** | Single-sided, Duplex (long-edge), Duplex (short-edge) |
| **Paper Size** | A4, A3, Letter, Legal, etc. |
| **Paper Source** | Tray 1, Tray 2, Auto-select |
| **Media Type** | Plain, Glossy, Cardstock, etc. |
| **Collate** | Collated or uncollated |
| **Fit to Page** | Scale document to fit paper size |
| **Job Ticketing** | Finishing options (staple, punch, bind) |

### Job Ticketing (Advanced Finishing)

For large or professional print jobs, specify finishing options:

- **Staple** — Top-left, top-right, dual, saddle
- **Punch** — 2-hole, 3-hole, 4-hole
- **Bind** — Tape bind, thermal bind, spiral
- **Fold** — Tri-fold, Z-fold, booklet

### Print Delegation (Print on Behalf)

You can print documents on behalf of another user if you have been granted delegation rights:

1. In the printer selection dialog, click **Delegation**
2. Search for and select the user
3. Select the pages to print
4. Confirm

The print job will be attributed to the delegated user.

---

## PDF Export

You can download your SafePages as a PDF document.

### Basic PDF Export

1. Select pages from your SafePages
2. Click **Download as PDF**
3. Save the PDF file

### PDF with Metadata

1. Click the **PDF Settings** or **Download Options** icon
2. Fill in metadata:
   - **Title** — Document title
   - **Author** — Author name
   - **Subject** — Document subject
   - **Keywords** — Comma-separated keywords
3. Click **Download**

### Encrypted PDF

For sensitive documents, you can password-protect the PDF:

1. In PDF Settings, enable **Encryption**
2. Set a user password and/or owner password
3. Download the encrypted PDF

---

## Email PDF

Send your SafePages as a PDF attachment via email:

1. Select pages from your SafePages
2. Click **Email PDF** or **Send by Email**
3. Enter the recipient email address
4. Add an optional message
5. Click **Send**

---

## User Details

The **User Details** section allows you to manage your personal settings.

### Changing Your Password

1. Go to **User Details** > **Change Password**
2. Enter your current password
3. Enter your new password twice
4. Click **Save**

### Setting Your Letterhead

As described above.

### Notification Preferences

Configure how you receive notifications:

- **Email notifications** — Enable/disable email alerts
- **Web App notifications** — Browser notifications
- **Desktop Client** — Real-time push via CometD

### Language Preference

Select your preferred language. PrintFlowLite supports multiple languages.

### Viewing Your Balance

If your organization tracks print costs, view your current balance and transaction history:

1. Go to **User Details** > **Financial**
2. View current balance
3. Click **Transaction History** for details

### Transaction History

View your print transactions:

| Column | Description |
|---|---|
| Date | Date and time of transaction |
| Description | Document name and action |
| Pages | Number of pages |
| Cost | Cost charged |
| Balance | Remaining balance |

---

## Web Print (Upload Files)

Upload files directly to your SafePages without printing from an application:

1. Click **Web Print** or **Upload** in the navigation
2. Drag and drop files onto the upload area, or click to browse
3. Wait for the file to be processed
4. The file appears in your SafePages

Supported file types: PDF, DOC, DOCX, ODT, images, and more.

### Web Print Limits

Your administrator may set limits on:

- Maximum file size
- Allowed file types
- Daily upload quota

---

## Mail Print

Email documents to PrintFlowLite for automatic import into your SafePages.

### Setting Up Mail Print

1. Note your personal Mail Print email address (found in User Details)
2. Email documents as attachments to this address
3. The documents appear in your SafePages

### Supported Attachments

- PDF
- DOC, DOCX
- Images (PNG, JPEG)
- And other common formats

---

## Internet Printer (Public IPP)

If your administrator has enabled Internet Printing, you can print to PrintFlowLite from outside the local network using a private Device URI.

### Finding Your Internet Printer URI

1. Go to **User Details** > **Internet Printer**
2. Copy the IPP or JetDirect URI

### Configuring Your Client

1. In your application's print dialog, add a new printer
2. Use the URI as the printer address
3. Authenticate with your PrintFlowLite username and app-key

---

## Troubleshooting Common Issues

### "Access Denied" when logging in

Check that your username and password are correct. Contact your administrator if you forgot your password.

### My print job doesn't appear in SafePages

1. Check that the printer name matches the PrintFlowLite queue name
2. Verify your authentication credentials are correct
3. Check the PrintFlowLite server is running
4. Try printing a test page

### I can't select a Proxy Printer

Only Proxy Printers configured by your administrator appear. Contact them if you need access to a specific printer.

### My balance is negative

Your administrator may allow negative balances temporarily. Contact your administrator or IT department to replenish your balance.

### Pages look wrong (orientation, size)

Check the print options in the Print dialog. Verify the paper size and orientation match your document.

### Letterhead doesn't appear

Ensure the letterhead is uploaded and selected in User Details. Some file types may not support letterhead overlay.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Space` | Select/deselect current page |
| `Ctrl+A` | Select all pages |
| `Delete` | Delete selected pages |
| `Ctrl+D` | Download selected as PDF |
| `Ctrl+P` | Open print dialog |
| `+` / `-` | Zoom in/out |
| `Left/Right Arrow` | Navigate pages |
