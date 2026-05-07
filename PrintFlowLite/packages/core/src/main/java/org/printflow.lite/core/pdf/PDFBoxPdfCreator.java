/*
 * This file is part of the PrintFlowLite project <https://printflowlite.local>.
 * Copyright (c) 2024 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2024 Datraverse B.V. <info@datraverse.com>
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
package org.printflow.lite.core.pdf;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.util.Matrix;
import org.printflow.lite.core.SpException;
import org.printflow.lite.core.UnavailableException;
import org.printflow.lite.core.community.CommunityDictEnum;
import org.printflow.lite.core.doc.DocContentToPdfException;
import org.printflow.lite.core.doc.IPdfConverter;
import org.printflow.lite.core.doc.PDFBoxPdfToAnnotatedURL;
import org.printflow.lite.core.doc.PdfToBooklet;
import org.printflow.lite.core.doc.PdfToEncryptedPdf;
import org.printflow.lite.core.doc.PdfToFilterImagePdf;
import org.printflow.lite.core.doc.PdfToRotateAlignedPdf;
import org.printflow.lite.core.doc.SvgToPdf;
import org.printflow.lite.core.i18n.PhraseEnum;
import org.printflow.lite.core.json.PdfProperties;
import org.printflow.lite.core.json.PdfProperties.PdfAllow;
import org.printflow.lite.core.services.ServiceContext;
import org.printflow.lite.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PDF Creator using the Apache PDFBox.
 *
 * @author Rijk Ravestein
 *
 */
public final class PDFBoxPdfCreator extends AbstractPdfCreator {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PDFBoxPdfCreator.class);

    /** */
    private static final String PDFBOX_NAME = "Apache PDFBox";
    /** */
    private boolean isAnnotateUrls = false;

    /** */
    private String targetPdfCopyFilePath;

    /** */
    private PDDocument targetDocument;

    /** */
    private File letterheadPdfFile;

    /** */
    private List<Overlay> overlayList = new ArrayList<>();

    /** */
    private List<File> filesToDelete = new ArrayList<File>();

    /**
     * The user rotate of the current job.
     */
    private Integer jobUserRotateWlk;

    /** */
    private boolean isRemoveGraphics = false;

    /** */
    private PDDocument documentWlk;

    /** */
    private Map<String, PDDocument> documentMap = new HashMap<>();

    /**
     * A set with one-based page numbers that need page orientation change to
     * align to orientation of first page in overall PDF document. If
     * {@code null} alignment action is not applicable.
     * <p>
     * Why would alignment be needed? No reason found for this PDFBox
     * implementation. So, alignment stays {@code null}.
     * </p>
     */
    @Deprecated
    private Set<Integer> targetPdfCopyPages2Align = null;

    /** */
    @Deprecated
    private Boolean firstPageSeenAsLandscape;

    /** */
    private boolean isStampEncryption = false;

    /** */
    private PdfProperties.PdfAllow pdfAllow;
    /** */
    private PdfProperties.PdfLinks pdfLinks;
    /** */
    private String pdfOwnerPass;
    /** */
    private String pdfUserPass;

    /**
     * {@code true} if the created pdf is to be converted to grayscale onExit.
     */
    private boolean onExitConvertToGrayscale = false;

    /**
     * {@code true} if the created pdf is to be converted to rasterized PDF
     * onExit.
     */
    private boolean onExitConvertToRaster = false;

    /**
     * {@code true} if PDF has to be repaired onExit.
     */
    private boolean onExitRepairPdf = false;

    /**
     * {@code true} if PDF with page porder for 2-up duplex booklet is to be
     * created.
     */
    private boolean onExitBookletPageOrder = false;

    /**
     * Gets the PageFormat from the {@link PDRectangle} media box.
     *
     * @param mediabox
     *            The mediabox {@link PDRectangle}.
     * @return The {@link PageFormat}.
     */
    private PageFormat createPageFormat(final PDRectangle mediabox) {

        final PageFormat pageFormat = new PageFormat();
        final Paper paper = new Paper();

        paper.setSize(mediabox.getWidth(), mediabox.getHeight());
        pageFormat.setPaper(paper);
        return pageFormat;
    }

    /**
     * Checks if a dynamic XFA form is present in PDF.
     *
     * @param pdfDocument
     *            The PDF document
     * @return {@code true} if a dynamic XFA form is present.
     */
    private static boolean
            isPdfXfaDynamicPresent(final PDDocument pdfDocument) {
        final PDAcroForm acroForm =
                pdfDocument.getDocumentCatalog().getAcroForm();
        return acroForm != null && acroForm.xfaIsDynamic();
    }

    /**
     * Gets the PDF page properties from the {@link PDRectangle} media box.
     *
     * @param mediabox
     *            The {@link PDRectangle} mediabox.
     * @return The {@link pPdfPageProps}.
     */
    private SpPdfPageProps createPageProps(final PDRectangle mediabox) {

        final PageFormat pageFormat = this.createPageFormat(mediabox);

        // NOTE: the size in returned in PORTRAIT mode.
        final int[] size = MediaUtils.getMediaWidthHeight(pageFormat);

        int iSizeWidth = 0;
        int iSizeHeight = 1;

        /*
         * Since the size is in portrait mode, we swap height and width when the
         * PDF mediabox reports landscape orientation.
         */
        if (mediabox.getWidth() > mediabox.getHeight()) {
            iSizeWidth = 1;
            iSizeHeight = 0;
        }

        final SpPdfPageProps pageProps = new SpPdfPageProps();

        pageProps.setMmWidth(size[iSizeWidth]);
        pageProps.setMmHeight(size[iSizeHeight]);
        pageProps.setSize(MediaUtils.getMediaSizeName(pageFormat));

        return pageProps;
    }

    /**
     * Checks PDF validity.
     *
     * @param filePathPdf
     * @throws IOException
     *             If invalid.
     */
    private static void checkPdfValidity(final String filePathPdf)
            throws IOException {

        try (RandomAccessRead accessFile =
                new RandomAccessReadBufferedFile(filePathPdf)) {
            final PDFParser parser = new PDFParser(accessFile);
            parser.parse(false); // throws IOException if PDF invalid.
        }
    }

    @Override
    protected int getNumberOfPagesInPdfFile(final String filePathPdf) {

        try (PDDocument document =
                Loader.loadPDF(new RandomAccessReadBufferedFile(filePathPdf))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new SpException(e);
        }
    }

    /**
     * @param doc
     *            PDF document.
     * @return Mmap of fonts by name present in a PDF.
     * @throws IOException
     *             If error.
     */
    private static Map<String, PDFont> getFontMap(final PDDocument doc)
            throws IOException {

        final Map<String, PDFont> fontMap = new HashMap<>();

        final PDPageTree ptree = doc.getDocumentCatalog().getPages();
        final Iterator<PDPage> iter = ptree.iterator();

        while (iter.hasNext()) {

            final PDPage pdPage = iter.next();

            final Iterator<COSName> fonts =
                    pdPage.getResources().getFontNames().iterator();

            while (fonts.hasNext()) {
                final COSName cosName = fonts.next();

                final PDFont font = pdPage.getResources().getFont(cosName);
                fontMap.put(cosName.getName(), font);
            }
        }
        return fontMap;
    }

    /**
     * Sets info and encryption of target document by copying it from source.
     *
     * @param docSrc
     *            source.
     * @param docTrg
     *            target.
     */
    public static void setInfoAndEncryption(final PDDocument docSrc,
            final PDDocument docTrg) {

        docTrg.setEncryptionDictionary(docSrc.getEncryption());

        final PDDocumentInformation src = docSrc.getDocumentInformation();
        final PDDocumentInformation trg = docTrg.getDocumentInformation();

        setProducerAndCreator(trg);

        trg.setAuthor(src.getAuthor());
        trg.setCreationDate(src.getCreationDate());
        trg.setKeywords(src.getKeywords());
        trg.setModificationDate(src.getModificationDate());
        trg.setSubject(src.getSubject());
        trg.setTitle(src.getTitle());
        trg.setTrapped(src.getTrapped());

    }

    /**
     * Creates the {@link PdfInfoDto} of a PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return {@link PdfInfoDto}
     */
    public static PdfInfoDto createPdfInfoDto(final String filePathPdf) {

        final PdfInfoDto dto = new PdfInfoDto();

        /*
         * Instantiating/opening can throw a InvalidPasswordException which are
         * subclasses of IOException: map these exception to our own variants.
         */
        try (PDDocument document =
                Loader.loadPDF(new RandomAccessReadBufferedFile(filePathPdf))) {

            final PDDocumentInformation info =
                    document.getDocumentInformation();

            dto.setAuthor(info.getAuthor());
            dto.setCreator(info.getCreator());
            dto.setFormat(Float.toString(document.getVersion()));
            dto.setProducer(info.getProducer());
            dto.setTitle(info.getTitle());

            final PdfInfoDto.Fonts dtoFonts = new PdfInfoDto.Fonts();
            dto.setFonts(dtoFonts);

            final Map<String, PDFont> fontMap = getFontMap(document);

            dtoFonts.setCount(fontMap.size());

            if (dtoFonts.getCount() > 0) {

                dtoFonts.setStandardEmbed(true);

                for (Entry<String, PDFont> entry : fontMap.entrySet()) {
                    final PDFont font = entry.getValue();
                    if (!font.isEmbedded() && !font.isStandard14()) {
                        dtoFonts.setStandardEmbed(false);
                        break;
                    }
                }
            }

        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
        return dto;
    }

    /**
     * Scales a PDF page to a (new) mediabox into a single page document.
     *
     * @param pageSrc
     *            Source page
     * @param mediaboxNew
     *            new mediabox.
     * @return A one-page document with the scaled page, or {@code null} if
     *         scaling was not performed because new mediabox is the same
     *         (within threshold) as source page.
     * @throws IOException
     *             If there is an error writing to the page contents.
     */
    public static PDDocument scaleToMediabox(final PDPage pageSrc,
            final PDRectangle mediaboxNew) throws IOException {

        final PDRectangle mediaboxSrc = pageSrc.getMediaBox();

        final float scaleFactor =
                mediaboxNew.getWidth() / mediaboxSrc.getWidth();

        final float scaleThreshold = 0.01f;

        final PDDocument resizedDoc;

        if (Math.abs(scaleFactor - 1.0f) > scaleThreshold) {

            resizedDoc = new PDDocument();
            resizedDoc.addPage(pageSrc);

            final PDPage overlayPage = resizedDoc.getPage(0);

            final PDPageContentStream contentStream =
                    new PDPageContentStream(resizedDoc, overlayPage,
                            PDPageContentStream.AppendMode.PREPEND, false);
            contentStream.transform(
                    Matrix.getScaleInstance(scaleFactor, scaleFactor));
            contentStream.close();
            overlayPage.setMediaBox(mediaboxNew);

        } else {
            resizedDoc = null;
        }
        return resizedDoc;
    }

    @Override
    public PdfInfoDto getPdfInfo(final String filePathPdf) {
        return createPdfInfoDto(filePathPdf);
    }

    @Override
    public SpPdfPageProps getPageProps(final String filePathPdf)
            throws PdfValidityException, PdfSecurityException,
            PdfPasswordException, PdfUnsupportedException {

        SpPdfPageProps pageProps = null;

        /*
         * Instantiating/opening can throw a InvalidPasswordException which are
         * subclasses of IOException: map these exception to our own variants.
         */
        try (PDDocument document =
                Loader.loadPDF(new RandomAccessReadBufferedFile(filePathPdf))) {

            if (document.isEncrypted()) {

                final AccessPermission permission = new AccessPermission(
                        document.getEncryption().getPermissions());

                final boolean isPrintingAllowed =
                        permission.canPrint() || permission.canPrintFaithful();

                final PhraseEnum phrase;
                if (isPrintingAllowed) {
                    phrase = PhraseEnum.PDF_ENCRYPTED_UNSUPPORTED;
                } else {
                    phrase = PhraseEnum.PDF_PRINTING_NOT_ALLOWED;
                }

                throw new PdfSecurityException(
                        phrase.uiText(ServiceContext.getLocale()), phrase,
                        isPrintingAllowed);
            }

            if (isPdfXfaDynamicPresent(document)) {
                throw new PdfUnsupportedException(
                        PhraseEnum.PDF_DYNAMIC_XFA_UNSUPPORTED
                                .uiText(Locale.ENGLISH),
                        PhraseEnum.PDF_DYNAMIC_XFA_UNSUPPORTED
                                .uiText(ServiceContext.getLocale()),
                        PhraseEnum.PDF_DYNAMIC_XFA_UNSUPPORTED);
            }

            final PDPage pageFirst = document.getPages().get(0);

            pageProps = this.createPageProps(pageFirst.getMediaBox());

            pageProps.setNumberOfPages(document.getNumberOfPages());
            pageProps.setRotationFirstPage(pageFirst.getRotation());

            final java.awt.geom.AffineTransform at =
                    PdfPageRotateHelper.getPdfPageCTM(pageFirst);
            pageProps.setContentRotationFirstPage(
                    PdfPageRotateHelper.getPageContentRotation(at).intValue());

            /*
             * Check validity.
             */
            checkPdfValidity(filePathPdf);

        } catch (InvalidPasswordException e) {
            throw new PdfPasswordException(
                    PhraseEnum.PDF_PASSWORD_UNSUPPORTED
                            .uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_PASSWORD_UNSUPPORTED);
        } catch (IOException e) {
            throw new PdfValidityException(e.getMessage(),
                    PhraseEnum.PDF_INVALID.uiText(ServiceContext.getLocale()),
                    PhraseEnum.PDF_INVALID);
        }

        return pageProps;
    }

    @Override
    protected void onInit() {

        this.onExitConvertToGrayscale = this.isGrayscalePdf();
        this.onExitConvertToRaster = this.isRasterizedPdf();
        this.onExitBookletPageOrder = this.isBookletPageOrder();
        this.onExitRepairPdf = this.isRepairPdf();

        this.targetPdfCopyFilePath = String.format("%s.tmp", this.pdfFile);

        if (this.isForPrinting() && this.isApplyLetterhead()) {
            /*
             * Why would alignment be needed in case of letterhead? No reason
             * found, so let alignment set be null.
             */
            this.targetPdfCopyPages2Align = null; // new HashSet<>();
        }

        this.firstPageSeenAsLandscape = null;

        this.isAnnotateUrls = this.isLinksPdf() && !this.isForPrinting()
                && !this.onExitConvertToRaster && !this.onExitBookletPageOrder;

        this.isStampEncryption = false;

        this.targetDocument = new PDDocument();
    }

    @Override
    protected void onExit() throws Exception {
        // no code intended
    }

    @Override
    protected void onInitJob(final String jobPdfName, final Integer userRotate)
            throws Exception {

        this.documentWlk = this.documentMap.get(jobPdfName);

        if (this.documentWlk == null) {
            this.documentWlk = Loader
                    .loadPDF(new RandomAccessReadBufferedFile(jobPdfName));
            this.documentMap.put(jobPdfName, this.documentWlk);
        }

        this.jobUserRotateWlk = userRotate;
    }

    @Override
    protected void onProcessJobPages(final int nPageFrom, final int nPageTo,
            final boolean removeGraphics) throws Exception {

        if (LOGGER.isTraceEnabled()) {
            final int nPagesOffset = this.targetDocument.getNumberOfPages();
            LOGGER.trace("onProcessJobPages {}-{}", nPagesOffset + nPageFrom,
                    nPagesOffset + nPageTo);
        }

        //
        this.isRemoveGraphics = removeGraphics;

        for (int nPage = nPageFrom; nPage <= nPageTo; nPage++) {

            this.targetDocument.addPage(this.documentWlk.getPage(nPage - 1));

            if (this.jobUserRotateWlk != null && this.jobUserRotateWlk != 0) {

                final int iPage = this.targetDocument.getNumberOfPages() - 1;

                final PDPage targetPage = this.targetDocument.getPage(iPage);

                final int rotation =
                        targetPage.getRotation() + this.jobUserRotateWlk;

                targetPage.setRotation(rotation);

                LOGGER.trace("  on page {}: setRotation({})", iPage + 1,
                        rotation);
            }

            if (this.targetPdfCopyPages2Align != null) {
                /*
                 * Lazy initialize on first page of first job.
                 */
                if (this.firstPageSeenAsLandscape == null) {
                    final PDPage firstPage = this.targetDocument.getPage(0);

                    this.firstPageFormat =
                            this.createPageFormat(firstPage.getMediaBox());

                    final java.awt.geom.AffineTransform at =
                            PdfPageRotateHelper.getPdfPageCTM(firstPage);

                    this.firstPageOrientationInfo =
                            PdfPageRotateHelper
                                    .getOrientationInfo(at,
                                            firstPage.getRotation(),
                                            PdfPageRotateHelper.isLandscapePage(
                                                    firstPage.getMediaBox()),
                                            0);

                    this.firstPageSeenAsLandscape =
                            PdfPageRotateHelper.isSeenAsLandscape(firstPage);
                } else {
                    final int nPageTarget =
                            this.targetDocument.getNumberOfPages();
                    final PDPage lastPage =
                            this.targetDocument.getPage(nPageTarget - 1);

                    if (!(this.firstPageSeenAsLandscape == PdfPageRotateHelper
                            .isSeenAsLandscape(lastPage))) {
                        this.targetPdfCopyPages2Align
                                .add(Integer.valueOf(nPageTarget));
                    }
                }
            }
        }
    }

    @Override
    protected void onExitJob(final int blankPagesToAppend) throws Exception {
        LOGGER.trace("onExitJob");
        /*
         * Add blank pages?
         */
        if (blankPagesToAppend > 0) {
            // Use page size of 1st page.
            final PDPage firstPage = this.targetDocument.getPage(0);
            final PDRectangle mediaBox = firstPage.getMediaBox();
            for (int i = 0; i < blankPagesToAppend; i++) {
                this.targetDocument.addPage(new PDPage(mediaBox));
            }
        }
    }

    @Override
    protected void onExitJobs() throws Exception {
        // no code intended.
    }

    @Override
    protected void onInitStamp() throws Exception {
    }

    /**
     * Applies the page overlays.
     *
     * @param pageOverlay
     *            Base64 encoded SVG overlay (value) for one-based ordinal pages
     *            (key) of the overall PDF document.
     * @throws IOException
     *             If IO error.
     */
    private void applySvgOverlay(final Map<Integer, String> pageOverlay)
            throws IOException {

        LOGGER.trace("applySvgOverlay");

        final SvgToPdf converter = new SvgToPdf();
        final HashMap<Integer, PDDocument> overlayDocMap = new HashMap<>();

        final List<PDDocument> docsToClose = new ArrayList<>();

        final int nDocPages = this.targetDocument.getNumberOfPages();
        int nPageWlk;

        for (int i = 0; i < nDocPages; i++) {

            nPageWlk = i + 1;

            final String svg64 = pageOverlay.get(Integer.valueOf(nPageWlk));

            if (svg64 == null) {
                continue;
            }

            final PDPage targetPage = this.targetDocument.getPage(i);
            final int targetPageRotation = targetPage.getRotation();

            final boolean swapWidhtHeightTarget =
                    isOverlayWidthHeightSwap(targetPageRotation);

            final PDRectangle mediaBoxPage = targetPage.getMediaBox();
            final PDRectangle mediaBoxTarget;

            if (swapWidhtHeightTarget) {
                mediaBoxTarget = new PDRectangle(0f, 0f,
                        mediaBoxPage.getHeight(), mediaBoxPage.getWidth());
            } else {
                mediaBoxTarget = mediaBoxPage;
            }

            final File tempFileSVG = File.createTempFile("temp-", ".svg");

            File overlayFilePDF = null;

            try (FileOutputStream fostr = new FileOutputStream(tempFileSVG)) {

                fostr.write(Base64.getDecoder().decode(svg64));
                fostr.close();

                overlayFilePDF = converter.convert(tempFileSVG, mediaBoxTarget);

                final PDDocument docOvl = Loader.loadPDF(overlayFilePDF);

                final PDDocument overlayDoc = new PDDocument();
                overlayDoc.addPage(docOvl.getPage(0));

                /*
                 * Rotate to align to target page rotation. Note: do not scale
                 * nor position on target page.
                 */
                final int rotation = -1 * targetPageRotation;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("  on page {}: setRotation({})", nPageWlk,
                            rotation);
                }

                final PDPage overlayPage = overlayDoc.getPage(0);
                overlayPage.setRotation(rotation);

                /*
                 * Put on overlay on map.
                 */
                overlayDocMap.put(nPageWlk, overlayDoc);
                /* Cleanup. */
                docsToClose.add(docOvl);

            } catch (DocContentToPdfException | UnavailableException e) {
                throw new SpException(e.getMessage());
            } finally {
                tempFileSVG.delete();
                if (overlayFilePDF != null) {
                    this.filesToDelete.add(overlayFilePDF);
                }
            }
        } // for-loop

        if (!overlayDocMap.isEmpty()) {

            final Overlay overlay = new Overlay();
            this.overlayList.add(overlay);

            overlay.setInputPDF(this.targetDocument);
            overlay.setOverlayPosition(Overlay.Position.FOREGROUND);
            overlay.overlayDocuments(overlayDocMap);

            // cleanup
            for (final File file : this.filesToDelete) {
                file.delete();
            }
            this.filesToDelete.clear();

            for (Entry<Integer, PDDocument> entry : overlayDocMap.entrySet()) {
                entry.getValue().close();
            }
            for (final PDDocument doc : docsToClose) {
                doc.close();
            }
        }
    }

    /**
     * Applies the letterhead.
     * <p>
     * If the letterhead document has more than one page, each page of the
     * letterhead is applied to the corresponding page of the output document.
     * If the output document has more pages than the letterhead, then the final
     * letterhead page is repeated across these remaining pages of the output
     * document.
     * </p>
     *
     * @return {@code true}, if applied letterhead has fonts.
     * @throws IOException
     *             If IO error.
     */
    private boolean applyLetterhead() throws IOException {

        LOGGER.trace("applyLetterhead");

        final Overlay.Position overlayPos;

        if (this.myLetterheadJob.getForeground()) {
            overlayPos = Overlay.Position.FOREGROUND;
        } else {
            overlayPos = Overlay.Position.BACKGROUND;
        }

        final List<PDDocument> modifiedLetterheadList = new ArrayList<>();

        boolean hasLetterheadFonts = false;

        try (PDDocument docLetterhead =
                Loader.loadPDF(this.letterheadPdfFile)) {

            hasLetterheadFonts = getFontMap(docLetterhead).size() > 0;

            final int nLetterheadPages = docLetterhead.getNumberOfPages();

            /*
             * Map with 1-based target document page (key) with its associated
             * single page letterhead document.
             */
            final HashMap<Integer, PDDocument> overlayDocMap = new HashMap<>();

            /*
             * Map with 1-based letterhead page number (key) with single-page
             * letterhead document for that page.
             */
            final HashMap<Integer, PDDocument> letterheadPageDocs =
                    new HashMap<>();

            int nLetterheadPageWlk = 0;

            for (int i = 0; i < this.targetDocument.getNumberOfPages(); i++) {

                if (nLetterheadPageWlk < nLetterheadPages) {
                    nLetterheadPageWlk++;
                }

                final PDDocument letterheadDoc;

                if (letterheadPageDocs.containsKey(nLetterheadPageWlk)) {
                    letterheadDoc = letterheadPageDocs.get(nLetterheadPageWlk);
                } else {
                    letterheadDoc = new PDDocument();
                    letterheadDoc.addPage(
                            docLetterhead.getPage(nLetterheadPageWlk - 1));

                    letterheadPageDocs.put(nLetterheadPageWlk, letterheadDoc);
                }

                final PDDocument letterheadDocModified =
                        this.applyLetterheadPage(this.targetDocument.getPage(i),
                                i, docLetterhead, nLetterheadPageWlk - 1);

                final PDDocument letterheadDocApplied;
                if (letterheadDocModified == null) {
                    letterheadDocApplied = letterheadDoc;
                } else {
                    letterheadDocApplied = letterheadDocModified;
                    modifiedLetterheadList.add(letterheadDocModified);
                }
                overlayDocMap.put(i + 1, letterheadDocApplied);
            }

            final Overlay overlay = new Overlay();
            this.overlayList.add(overlay);

            overlay.setInputPDF(this.targetDocument);
            overlay.setOverlayPosition(overlayPos);
            overlay.overlayDocuments(overlayDocMap);

            // cleanup
            for (Entry<Integer, PDDocument> entry : overlayDocMap.entrySet()) {
                entry.getValue().close();
            }

            for (PDDocument doc : modifiedLetterheadList) {
                doc.close();
            }
        }

        return hasLetterheadFonts;
    }

    /**
     * Checks if width/height of overlay needs to be swapped on target page as
     * perceived in PDF viewer.
     *
     * @param targetPageRotation
     *            rotation of target page.
     * @return {@code true} if swapped.
     */
    private static boolean
            isOverlayWidthHeightSwap(final int targetPageRotation) {
        final boolean swapWidhtHeightTarget;

        if (targetPageRotation == PdfPageRotateHelper.PDF_ROTATION_0.intValue()
                || targetPageRotation == PdfPageRotateHelper.PDF_ROTATION_180
                        .intValue()) {

            swapWidhtHeightTarget = false;

        } else if (targetPageRotation == PdfPageRotateHelper.PDF_ROTATION_90
                .intValue()
                || targetPageRotation == PdfPageRotateHelper.PDF_ROTATION_270
                        .intValue()) {

            swapWidhtHeightTarget = true;

        } else {
            throw new IllegalStateException(String
                    .format("Invalid page rotation [%d]", targetPageRotation));
        }
        return swapWidhtHeightTarget;
    }

    /**
     * Apply letterhead page.
     *
     * @param targetPage
     * @param iTargetPage
     *            0-based target page ordinal.
     * @param docLetterheadOrg
     *            The original (vanilla) full-page letterhead document that is
     *            not adapted to any target page. or repositioned..
     * @param iSelectedLetterheadPage
     *            0-based letterhead page ordinal.
     * @return newly created {@link PDDocument} with letterhead page adapted to
     *         the target page, or {@code null} if page from original full-page
     *         letterhead document is adopted.
     * @throws IOException
     *             If IO error.
     */
    private PDDocument applyLetterheadPage(final PDPage targetPage,
            final int iTargetPage, final PDDocument docLetterheadOrg,
            final int iSelectedLetterheadPage) throws IOException {

        final int targetPageRotation = targetPage.getRotation();

        final PDRectangle mediaboxTarget = targetPage.getMediaBox();

        if (targetPageRotation == PdfPageRotateHelper.ROTATION_0) {

            final PDRectangle mediaboxLetterhead = docLetterheadOrg
                    .getPage(iSelectedLetterheadPage).getMediaBox();

            // Compare with rounded width and height (ignore decimal fractions).
            if (Math.round(mediaboxLetterhead.getHeight()) == Math
                    .round(mediaboxTarget.getHeight())
                    && Math.round(mediaboxLetterhead.getWidth()) == Math
                            .round(mediaboxTarget.getWidth())) {
                // no processing needed, use candidate page.
                return null;
            }
        }

        // Create new letterhead instance.
        final PDDocument docLetterheadAllPages =
                Loader.loadPDF(this.letterheadPdfFile);

        final PDDocument docLetterheadSinglePage = new PDDocument();

        docLetterheadSinglePage.addPage(
                docLetterheadAllPages.getPage(iSelectedLetterheadPage));

        /*
         * Get width x height of target page as perceived in PDF viewer.
         */
        final boolean swapWidhtHeightTarget =
                isOverlayWidthHeightSwap(targetPageRotation);

        final float widthTargetPerceived;
        final float heightTargetPerceived;

        if (swapWidhtHeightTarget) {
            widthTargetPerceived = mediaboxTarget.getHeight();
            heightTargetPerceived = mediaboxTarget.getWidth();
        } else {
            widthTargetPerceived = mediaboxTarget.getWidth();
            heightTargetPerceived = mediaboxTarget.getHeight();
        }

        final PDPage letterheadPage = docLetterheadSinglePage.getPage(0);
        final PDRectangle mediaboxLetterhead = letterheadPage.getMediaBox();

        /*
         * Apply target page rotation to letterhead.
         */
        if (targetPageRotation != PdfPageRotateHelper.ROTATION_0) {

            final int letterheadRotation =
                    letterheadPage.getRotation() - targetPageRotation;

            letterheadPage.setRotation(letterheadRotation);
        }

        final float widthLetterhead = mediaboxLetterhead.getWidth();
        final float heightLetterhead = mediaboxLetterhead.getHeight();

        final float widthDiff = widthTargetPerceived - widthLetterhead;
        final float heightDiff = heightTargetPerceived - heightLetterhead;

        /*
         * Scale letterhead to fit target page.
         */
        float scale = 1.0f;

        if (widthDiff != 0 || heightDiff != 0) {

            float xScale = widthTargetPerceived / widthLetterhead;
            float yScale = heightTargetPerceived / heightLetterhead;

            // Use smallest scale factor.
            if (xScale < yScale) {
                scale = xScale;
            } else {
                scale = yScale;
            }

            PdfPageRotateHelper.scalePage(docLetterheadSinglePage, 0, scale);
        }

        /*
         * Position on target.
         */
        final float widthLetterheadScaled =
                mediaboxLetterhead.getWidth() * scale;

        letterheadPage.setMediaBox(this.calcScaledOverlayPositionOnTarget(
                widthTargetPerceived, heightTargetPerceived, widthLetterhead,
                widthLetterheadScaled));

        return docLetterheadSinglePage;
    }

    /**
     * Calculates the position of scaled overlay on a target page. Note: overlay
     * is positioned with LowerLeftY == 0.
     *
     * @param widthTarget
     *            Target width.
     * @param heightTarget
     *            Target height.
     * @param widthOverlay
     *            Overlay width before scaling.
     * @param widthOverlayScaled
     *            Overlay <i>after</i> scaling.
     * @return {@link PDRectangle} with overlay position relative to target.
     */
    private PDRectangle calcScaledOverlayPositionOnTarget(
            final float widthTarget, final float heightTarget,
            final float widthOverlay, final float widthOverlayScaled) {

        final float widthDiff = widthTarget - widthOverlay;
        final float paddingX;
        if (widthDiff < 0) {
            paddingX = 0;
        } else {
            final float widthDiffScaled = widthTarget - widthOverlayScaled;
            paddingX = widthDiffScaled / 2.0f;
        }

        final PDRectangle mediaBoxNew = new PDRectangle();

        mediaBoxNew.setLowerLeftX(paddingX);
        mediaBoxNew.setLowerLeftY(0);

        mediaBoxNew.setUpperRightX(widthTarget - paddingX);
        mediaBoxNew.setUpperRightY(heightTarget);

        return mediaBoxNew;
    }

    @Override
    protected void onExitStamp(final Map<Integer, String> pageOverlay)
            throws Exception {
        LOGGER.trace("onExitStamp");

        if (!pageOverlay.isEmpty()) {
            this.applySvgOverlay(pageOverlay);
        }

        if (this.isApplyLetterhead() && this.letterheadPdfFile != null) {
            this.applyLetterhead();
        }

    }

    @Override
    protected void onStampLetterhead(final String pdfLetterhead)
            throws Exception {
        LOGGER.trace("onStampLetterhead");
        this.letterheadPdfFile = new File(pdfLetterhead);
    }

    @Override
    protected void onCompress() throws Exception {
        // no code intended.
    }

    @Override
    protected void onProcessFinally() {

        try {
            // 1: save target PDF.
            this.targetDocument.save(this.targetPdfCopyFilePath);

            // close merged PDF's.
            for (Entry<String, PDDocument> entry : this.documentMap
                    .entrySet()) {
                entry.getValue().close();
            }
            // close overlays
            if (this.overlayList != null) {
                for (final Overlay ovl : this.overlayList) {
                    ovl.close();
                }
            }

            // delete temp files
            for (final File file : this.filesToDelete) {
                file.delete();
            }
            this.filesToDelete.clear();

            // close target PDF.
            this.targetDocument.close();

            if (this.isRemoveGraphics) {
                final File generatedPdf = new File(this.targetPdfCopyFilePath);
                replaceWithConvertedPdf(generatedPdf,
                        new PdfToFilterImagePdf().convert(generatedPdf));
            }

            Files.move(Path.of(this.targetPdfCopyFilePath),
                    Path.of(this.pdfFile));

        } catch (IOException e) {
            throw new SpException(e);
        }
    }

    @Override
    protected void onPdfProperties(final PdfProperties propPdf) {
        this.pdfLinks = propPdf.getLinks();
    }

    @Override
    protected void onPdfGenerated(final File pdfFile) throws Exception {

        onPdfGeneratedCmd(pdfFile, this.onExitConvertToRaster,
                this.getRasterizedResolution(), this.onExitConvertToGrayscale,
                this.onExitRepairPdf);

        if (this.onExitBookletPageOrder) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToBooklet().convert(pdfFile));
        }

        /*
         * Ad-hoc rotate?
         */
        if (this.targetPdfCopyPages2Align != null
                && this.targetPdfCopyPages2Align.size() > 0) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToRotateAlignedPdf(this.firstPageSeenAsLandscape,
                            this.targetPdfCopyPages2Align).convert(pdfFile));
        }

        /*
         * Annotate URLs including Letterhead. Note: optional PDF encryption is
         * part of last action.
         */
        if (this.isAnnotateUrls) {

            final IPdfConverter converter;

            if (this.isStampEncryption) {
                converter = new PDFBoxPdfToAnnotatedURL(this.pdfAllow,
                        this.pdfLinks, this.pdfOwnerPass, this.pdfUserPass);
            } else {
                converter = new PDFBoxPdfToAnnotatedURL(this.pdfLinks);
            }

            replaceWithConvertedPdf(pdfFile, converter.convert(pdfFile));

        } else if (this.isStampEncryption) {
            replaceWithConvertedPdf(pdfFile,
                    new PdfToEncryptedPdf(this.pdfAllow, this.pdfOwnerPass,
                            this.pdfUserPass).convert(pdfFile));
        }
    }

    /**
     * Sets the producer in PD document info.
     *
     * @param docInfo
     *            PDF info.
     */
    public static void setProducer(final PDDocumentInformation docInfo) {
        docInfo.setProducer(String.format("%s %s", PDFBOX_NAME,
                org.apache.pdfbox.util.Version.getVersion()));
    }

    /**
     * Sets the producer and creator in PD document info.
     *
     * @param docInfo
     *            PDF info.
     */
    public static void
            setProducerAndCreator(final PDDocumentInformation docInfo) {
        docInfo.setProducer(String.format("%s %s", PDFBOX_NAME,
                org.apache.pdfbox.util.Version.getVersion()));
        docInfo.setCreator(getCreatorString());
    }

    /**
     *
     * @param now
     * @param propPdf
     * @param author
     * @param subject
     */
    private void onStampMetaDataForExportEx(final Calendar now,
            final PdfProperties propPdf, final String author,
            final String subject) {

        final PDDocumentInformation docInfo =
                this.targetDocument.getDocumentInformation();

        setProducer(docInfo);

        docInfo.setTitle(propPdf.getDesc().getTitle());

        if (subject != null) {
            docInfo.setSubject(subject);
        }

        docInfo.setAuthor(author);

        docInfo.setCreationDate(now);
        docInfo.setModificationDate(now);

        docInfo.setCreator(getCreatorString());

        if (propPdf.getApply().getKeywords()) {
            docInfo.setKeywords(propPdf.getDesc().getKeywords());
        }
    }

    @Override
    protected void onStampMetaDataForExport(final Calendar now,
            final PdfProperties propPdf) {

        final String subject;

        if (propPdf.getApply().getSubject()) {
            subject = propPdf.getDesc().getSubject();
        } else {
            subject = null;
        }

        this.onStampMetaDataForExportEx(now, propPdf,
                propPdf.getDesc().getAuthor(), subject);
    }

    @Override
    protected void onStampMetaDataForPrinting(final Calendar now,
            final PdfProperties propPdf) {

        this.onStampMetaDataForExportEx(now, propPdf,
                CommunityDictEnum.PrintFlowLite.getWord(),
                PDF_INFO_SUBJECT_FOR_PRINTING);
    }

    @Override
    protected void onStampEncryptionForExport(final PdfAllow propPdfAllow,
            final String ownerPass, final String userPass) {
        this.isStampEncryption = true;
        this.pdfAllow = propPdfAllow;
        this.pdfOwnerPass = ownerPass;
        this.pdfUserPass = userPass;
    }

}
