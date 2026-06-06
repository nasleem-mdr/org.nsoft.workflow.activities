/*******************************************************************************
 * WFTransactionDetailRenderer.java
 *
 * Renderer class to query document details
 * at Workflow Approval (WWFActivity) Form
 *
 * Architecture : SysConfig-driven + Generic PO + SQL Query
 * Versi       : 2.0 — fix ORDER_BY, validasi config, edge case qty/amt
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * SYSCONFIG KEY PATTERN : WF_DETAIL_<TableName><Suffix>
 * ─────────────────────────────────────────────────────────────────────────────
 * Suffix         Wajib   Keterangan                        Contoh Value
 * _LINE_TABLE    N       Nama tabel line                   C_OrderLine
 * _LINK_COL      N*      FK kolom di tabel line            C_Order_ID
 * _ORDER_BY      N       Order by query lines              Line,C_Order_ID
 * _DESC_COL      N       Kolom deskripsi (bisa FK)         M_Product_ID>Name,Description
 * _QTY_COL       N       Kolom qty (dicoba berurutan)      QtyOrdered,QtyEntered
 * _AMT_COL       N       Kolom amount (dicoba berurutan)   LineNetAmt,PriceActual
 * _HDR_DOCNO     N       Kolom nomor dokumen               DocumentNo
 * _HDR_BP        N       Kolom BP (bisa FK)                C_BPartner_ID>Name
 * _HDR_DATE      N       Kolom tanggal (dicoba berurutan)  DateOrdered,DateDoc
 * _HDR_TOTAL     N       Kolom grand total                 GrandTotal,TotalLines
 *
 * *) LINK_COL wajib jika LINE_TABLE diisi
 *
 * Gunakan "-" sebagai value untuk menonaktifkan kolom tertentu.
 * Jika SysConfig tidak dikonfigurasi sama sekali, renderer tetap
 * tampil dengan nilai fallback generik — tidak crash.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CONTOH KONFIGURASI LENGKAP
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * C_Order:
 *   WF_DETAIL_C_Order_LINE_TABLE  = C_OrderLine
 *   WF_DETAIL_C_Order_LINK_COL   = C_Order_ID
 *   WF_DETAIL_C_Order_ORDER_BY   = Line,C_Order_ID
 *   WF_DETAIL_C_Order_DESC_COL   = M_Product_ID>Name,Description
 *   WF_DETAIL_C_Order_QTY_COL    = QtyOrdered,QtyEntered
 *   WF_DETAIL_C_Order_AMT_COL    = LineNetAmt
 *   WF_DETAIL_C_Order_HDR_DOCNO  = DocumentNo
 *   WF_DETAIL_C_Order_HDR_BP     = C_BPartner_ID>Name
 *   WF_DETAIL_C_Order_HDR_DATE   = DateOrdered,DateDoc
 *   WF_DETAIL_C_Order_HDR_TOTAL  = GrandTotal
 *
 * C_Invoice:
 *   WF_DETAIL_C_Invoice_LINE_TABLE  = C_InvoiceLine
 *   WF_DETAIL_C_Invoice_LINK_COL   = C_Invoice_ID
 *   WF_DETAIL_C_Invoice_ORDER_BY   = Line,C_Invoice_ID
 *   WF_DETAIL_C_Invoice_DESC_COL   = M_Product_ID>Name,Description
 *   WF_DETAIL_C_Invoice_QTY_COL    = QtyInvoiced
 *   WF_DETAIL_C_Invoice_AMT_COL    = LineNetAmt
 *   WF_DETAIL_C_Invoice_HDR_DOCNO  = DocumentNo
 *   WF_DETAIL_C_Invoice_HDR_BP     = C_BPartner_ID>Name
 *   WF_DETAIL_C_Invoice_HDR_DATE   = DateInvoiced,DateDoc
 *   WF_DETAIL_C_Invoice_HDR_TOTAL  = GrandTotal
 *
 * M_InOut:
 *   WF_DETAIL_M_InOut_LINE_TABLE  = M_InOutLine
 *   WF_DETAIL_M_InOut_LINK_COL   = M_InOut_ID
 *   WF_DETAIL_M_InOut_ORDER_BY   = Line,M_InOut_ID
 *   WF_DETAIL_M_InOut_DESC_COL   = M_Product_ID>Name,Description
 *   WF_DETAIL_M_InOut_QTY_COL    = MovementQty,QtyEntered
 *   WF_DETAIL_M_InOut_AMT_COL    = -
 *   WF_DETAIL_M_InOut_HDR_DOCNO  = DocumentNo
 *   WF_DETAIL_M_InOut_HDR_BP     = C_BPartner_ID>Name
 *   WF_DETAIL_M_InOut_HDR_DATE   = MovementDate,DateDoc
 *   WF_DETAIL_M_InOut_HDR_TOTAL  = -
 *
 * M_Requisition:
 *   WF_DETAIL_M_Requisition_LINE_TABLE  = M_RequisitionLine
 *   WF_DETAIL_M_Requisition_LINK_COL   = M_Requisition_ID
 *   WF_DETAIL_M_Requisition_ORDER_BY   = Line,M_Requisition_ID
 *   WF_DETAIL_M_Requisition_DESC_COL   = M_Product_ID>Name,Description
 *   WF_DETAIL_M_Requisition_QTY_COL    = Qty
 *   WF_DETAIL_M_Requisition_AMT_COL    = LineNetAmt
 *   WF_DETAIL_M_Requisition_HDR_DOCNO  = DocumentNo
 *   WF_DETAIL_M_Requisition_HDR_BP     = C_BPartner_ID>Name
 *   WF_DETAIL_M_Requisition_HDR_DATE   = DateRequired,DateDoc
 *   WF_DETAIL_M_Requisition_HDR_TOTAL  = TotalLines
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CHANGELOG
 * ─────────────────────────────────────────────────────────────────────────────
 * v2.0 - Fix ORDER_BY configurable agar tidak crash jika kolom Line tidak ada
 *      - Tambah validateAndWarnConfig() untuk deteksi salah konfigurasi
 *      - Tambah isNumericValue() untuk validasi qty/amt sebelum format
 *      - Tambah sanitizeNumeric() untuk handle nilai non-numerik dengan aman
 *      - Tambah _ORDER_BY sebagai SysConfig key baru
 *      - resolveColumnValue() lebih defensif terhadap nilai whitespace
 *      - formatAmount() tidak lagi silent — log warning jika nilai non-numerik
 * ─────────────────────────────────────────────────────────────────────────────
 ******************************************************************************/
package org.nsoft.workflow.activities;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MTable;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.wf.MWFActivity;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.compiere.model.MQuery;


public class WFTransactionDetailRenderer {

    // =========================================================================
    // LOGGER
    // =========================================================================
    private static final CLogger log = CLogger.getCLogger(WFTransactionDetailRenderer.class);

    // =========================================================================
    // SYSCONFIG KEY CONSTANTS
    // =========================================================================
    private static final String PREFIX      = "WF_DETAIL_";
    private static final String LINE_TABLE  = "_LINE_TABLE";
    private static final String LINK_COL    = "_LINK_COL";
    private static final String ORDER_BY    = "_ORDER_BY";   // v2.0: configurable order
    private static final String DESC_COL    = "_DESC_COL";
    private static final String QTY_COL     = "_QTY_COL";
    private static final String AMT_COL     = "_AMT_COL";
    private static final String HDR_DOCNO   = "_HDR_DOCNO";
    private static final String HDR_BP      = "_HDR_BP";
    private static final String HDR_DATE    = "_HDR_DATE";
    private static final String HDR_TOTAL   = "_HDR_TOTAL";

    // =========================================================================
    // DEFAULT FALLBACK VALUES
    // Dipakai jika SysConfig tidak dikonfigurasi untuk tabel tertentu.
    // Urutan penting — dicoba dari kiri ke kanan, berhenti di yang pertama ada.
    // =========================================================================
    private static final String DEFAULT_DOCNO  = "DocumentNo,Value,Name";
    private static final String DEFAULT_BP     = "C_BPartner_ID>Name";
    private static final String DEFAULT_DATE   = "DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created";
    private static final String DEFAULT_TOTAL  = "GrandTotal,TotalLines,-";
    private static final String DEFAULT_DESC   = "M_Product_ID>Name,Description,Name";
    private static final String DEFAULT_QTY    = "QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty";
    private static final String DEFAULT_AMT    = "LineNetAmt,PriceActual,-";
    // v2.0: ORDER_BY default hanya pakai linkCol (safe untuk semua tabel)
    // akan di-resolve dinamis di renderLines() karena butuh linkCol
    private static final String ORDER_BY_SAFE_FALLBACK = null; // resolved dinamis

    // =========================================================================
    // UI COMPONENTS — disuntikkan dari WWFActivity via constructor
    // =========================================================================
    private final Groupbox grpTxDetails;
    private final Listbox  lstTxLines;
    private final Label    lHdrDocNo;
    private final Label    lHdrDateDoc;
    private final Label    lHdrBPName;
    private final Label    lHdrGrandTotal;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public WFTransactionDetailRenderer(
            Groupbox grpTxDetails,
            Listbox  lstTxLines,
            Label    lHdrDocNo,
            Label    lHdrDateDoc,
            Label    lHdrBPName,
            Label    lHdrGrandTotal) {

        this.grpTxDetails   = grpTxDetails;
        this.lstTxLines     = lstTxLines;
        this.lHdrDocNo      = lHdrDocNo;
        this.lHdrDateDoc    = lHdrDateDoc;
        this.lHdrBPName     = lHdrBPName;
        this.lHdrGrandTotal = lHdrGrandTotal;
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Entry point utama. Dipanggil dari WWFActivity.display() setiap kali
     * item approval dipilih di listbox kiri.
     *
     * @param activity MWFActivity aktif, boleh null
     */
    public void render(MWFActivity activity) {
        clearUI();

        if (activity == null || activity.getRecord_ID() <= 0) {
            grpTxDetails.setVisible(false);
            return;
        }

        try {
            int    clientId  = activity.getAD_Client_ID();
            int    tableId   = activity.getAD_Table_ID();
            int    recordId  = activity.getRecord_ID();
            MTable table     = MTable.get(Env.getCtx(), tableId);
            String tableName = table.getTableName();

            // v2.0: Validasi konfigurasi — log warning jika ada yang kurang tepat
            validateAndWarnConfig(tableName, clientId);

            PO headerPO = table.getPO(recordId, null);
            if (headerPO == null) {
            	org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption("Detail (" + tableName + " #" + recordId + " tidak ditemukan)");
            	grpTxDetails.appendChild(cp);
                grpTxDetails.setVisible(true);
                return;
            }

            renderHeader(headerPO, tableName, clientId);
            renderLines(headerPO, tableName, recordId, clientId);

            grpTxDetails.setVisible(true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "WFTransactionDetailRenderer.render() gagal", e);
            org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption("Detail (Error: " + e.getMessage() + ")");
            grpTxDetails.appendChild(cp);
            grpTxDetails.setVisible(true);
        }
    }

    // =========================================================================
    // PRIVATE — RENDER HEADER
    // =========================================================================

    private void renderHeader(PO headerPO, String tableName, int clientId) {

        // Doc No
        String docNoCfg = getSysConfig(tableName, HDR_DOCNO, clientId, DEFAULT_DOCNO);
        String docNo    = resolveColumnValue(headerPO, docNoCfg);
        if (isEmpty(docNo))
            docNo = tableName + " #" + headerPO.get_ID();
        lHdrDocNo.setValue(docNo);

        // Date
        String dateCfg = getSysConfig(tableName, HDR_DATE, clientId, DEFAULT_DATE);
        Object rawDate = resolveColumnObject(headerPO, dateCfg);
        lHdrDateDoc.setValue(rawDate != null ? formatDate(rawDate) : "-");

        // Business Partner
        String bpCfg  = getSysConfig(tableName, HDR_BP, clientId, DEFAULT_BP);
        String bpName = resolveColumnValue(headerPO, bpCfg);
        if (isEmpty(bpName))
            bpName = resolveCreatedByName(headerPO);
        lHdrBPName.setValue(bpName);

        // Grand Total
        String totalCfg = getSysConfig(tableName, HDR_TOTAL, clientId, DEFAULT_TOTAL);
        String total    = resolveColumnValue(headerPO, totalCfg);

        // v2.0: validasi apakah nilai total benar-benar numerik
        if (!isEmpty(total) && !"-".equals(total))
            lHdrGrandTotal.setValue(formatAmount(total, "GrandTotal[" + tableName + "]"));
        else
            lHdrGrandTotal.setValue("-");

        org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption("Detail: " + tableName + "  #" + headerPO.get_ID());
        grpTxDetails.appendChild(cp);
    }

    // =========================================================================
    // PRIVATE — RENDER LINES
    // =========================================================================

    private void renderLines(PO headerPO, String tableName, int recordId, int clientId) {

        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        String linkCol   = getSysConfig(tableName, LINK_COL,   clientId, null);

        // Tidak ada konfigurasi line table — sembunyikan section lines, tidak error
        if (isEmpty(lineTable) || "-".equals(lineTable)) {
            lstTxLines.setVisible(false);
            return;
        }

        // LINE_TABLE ada tapi LINK_COL kosong — ini kesalahan konfigurasi
        // Sudah di-log oleh validateAndWarnConfig(), tampilkan pesan ke UI
        if (isEmpty(linkCol) || "-".equals(linkCol)) {
            lstTxLines.setVisible(true);
            appendInfoRow("Konfigurasi tidak lengkap: LINK_COL untuk " + tableName + " belum diisi.");
            return;
        }

        String descCfg = getSysConfig(tableName, DESC_COL, clientId, DEFAULT_DESC);
        String qtyCfg  = getSysConfig(tableName, QTY_COL,  clientId, DEFAULT_QTY);
        String amtCfg  = getSysConfig(tableName, AMT_COL,  clientId, DEFAULT_AMT);

        // v2.0: ORDER_BY configurable — fallback ke linkCol saja jika tidak dikonfigurasi
        // Ini aman karena linkCol pasti ada di tabel line sebagai FK
        String orderByCfg = getSysConfig(tableName, ORDER_BY, clientId, linkCol);

        lstTxLines.setVisible(true);

        try {
            List<PO> lines = new Query(Env.getCtx(), lineTable,
                    linkCol + " = ?", null)
                    .setParameters(recordId)
                    .setOrderBy(orderByCfg)
                    .list();

            if (lines == null || lines.isEmpty()) {
                appendInfoRow("(Tidak ada baris detail)");
                return;
            }

            for (PO line : lines) {

                // 1. Description / Product name
                String desc = resolveColumnValue(line, descCfg);
                if (isEmpty(desc)) desc = "Item #" + line.get_ID();

                // 2. Qty — v2.0: sanitize jika nilai adalah non-numerik
                String qtyRaw = resolveColumnValue(line, qtyCfg);
                String qty    = sanitizeNumeric(qtyRaw,
                        "QTY_COL[" + tableName + "] line#" + line.get_ID());

                // 3. Amount — v2.0: sanitize + format jika numerik
                String amtRaw = resolveColumnValue(line, amtCfg);
                String amt;
                if (isEmpty(amtRaw) || "-".equals(amtRaw)) {
                    amt = "-";
                } else {
                    String sanitized = sanitizeNumeric(amtRaw,
                            "AMT_COL[" + tableName + "] line#" + line.get_ID());
                    amt = isNumericString(sanitized)
                            ? formatAmount(sanitized, "AMT[" + tableName + "]")
                            : sanitized; // tampil apa adanya jika string
                }

                Listitem item = new Listitem();
                item.appendChild(new Listcell(desc));
                item.appendChild(new Listcell(qty));
                item.appendChild(new Listcell(amt));
                lstTxLines.appendChild(item);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "renderLines gagal: lineTable=" + lineTable
                    + " linkCol=" + linkCol + " orderBy=" + orderByCfg, e);
            appendInfoRow("Error memuat baris: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE — VALIDASI KONFIGURASI (v2.0)
    // Dipanggil sekali per render() — hanya log warning, tidak throw exception
    // =========================================================================

    /**
     * Periksa kelengkapan konfigurasi SysConfig untuk tabel yang sedang dirender.
     * Tidak memblokir eksekusi — hanya memberi peringatan di log untuk admin.
     */
    private void validateAndWarnConfig(String tableName, int clientId) {
        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);

        // Tidak ada konfigurasi sama sekali — wajar, tidak perlu warning
        if (lineTable == null) return;

        // Ada LINE_TABLE tapi LINK_COL kosong
        String linkCol = getSysConfig(tableName, LINK_COL, clientId, null);
        if (isEmpty(linkCol) || "-".equals(linkCol)) {
            log.warning("[WFDetail] " + tableName
                    + ": LINE_TABLE=" + lineTable
                    + " dikonfigurasi tapi LINK_COL kosong atau '-'. Lines tidak akan ditampilkan.");
        }

        // LINE_TABLE diisi "-" — sengaja dinonaktifkan, OK
        if ("-".equals(lineTable)) return;

        // QTY_COL tidak dikonfigurasi — akan pakai DEFAULT, beri info
        String qtyCfg = getSysConfig(tableName, QTY_COL, clientId, null);
        if (qtyCfg == null) {
            log.info("[WFDetail] " + tableName
                    + ": QTY_COL tidak dikonfigurasi, pakai default: " + DEFAULT_QTY);
        }

        // AMT_COL tidak dikonfigurasi — akan pakai DEFAULT, beri info
        String amtCfg = getSysConfig(tableName, AMT_COL, clientId, null);
        if (amtCfg == null) {
            log.info("[WFDetail] " + tableName
                    + ": AMT_COL tidak dikonfigurasi, pakai default: " + DEFAULT_AMT);
        }

        // ORDER_BY tidak dikonfigurasi — akan pakai LINK_COL sebagai fallback, beri info
        String orderBy = getSysConfig(tableName, ORDER_BY, clientId, null);
        if (orderBy == null) {
            log.info("[WFDetail] " + tableName
                    + ": ORDER_BY tidak dikonfigurasi, fallback ke LINK_COL=" + linkCol);
        }
    }

    // =========================================================================
    // PRIVATE — SYSCONFIG READER
    // =========================================================================

    /**
     * Baca SysConfig dengan key: WF_DETAIL_<TableName><suffix>
     * Return defaultValue jika key tidak ada atau valuenya kosong/whitespace.
     */
    private String getSysConfig(String tableName, String suffix,
                                int clientId, String defaultValue) {
        String key   = PREFIX + tableName + suffix;
        String value = MSysConfig.getValue(key, null, clientId);
        return (value != null && !value.trim().isEmpty())
                ? value.trim()
                : defaultValue;
    }

    // =========================================================================
    // PRIVATE — COLUMN RESOLVERS
    // =========================================================================

    /**
     * Resolve nilai String dari daftar kolom (comma-separated).
     * Dicoba satu per satu, return nilai pertama yang tidak kosong/whitespace.
     *
     * Mendukung notasi FK: "C_BPartner_ID>Name"
     * Untuk skip    : "-"
     */
    private String resolveColumnValue(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : columnDefs.split(",")) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;

            String value = resolveOneColumn(po, candidate);
            // v2.0: trim hasil sebelum isEmpty check
            if (value != null && !value.trim().isEmpty())
                return value.trim();
        }
        return null;
    }

    /**
     * Resolve Object asli dari kolom non-FK.
     * Digunakan untuk mendapatkan raw Date object sebelum diformat.
     */
    private Object resolveColumnObject(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : columnDefs.split(",")) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;
            if (candidate.contains(">")) continue; // skip FK untuk date

            Object val = po.get_Value(candidate);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * Resolve satu definisi kolom.
     *
     * Format biasa : "DocumentNo"
     * Format FK    : "C_BPartner_ID>Name"
     *
     * Untuk FK, nama tabel di-derive dari nama kolom:
     *   C_BPartner_ID → tabel C_BPartner
     *   M_Product_ID  → tabel M_Product
     *   AD_User_ID    → tabel AD_User
     */
    private String resolveOneColumn(PO po, String columnDef) {
        if (columnDef == null || columnDef.isEmpty()) return null;

        if (columnDef.contains(">")) {
            // FK Lookup
            String[] parts = columnDef.split(">", 2);
            if (parts.length != 2) return null;

            String fkCol     = parts[0].trim();
            String targetCol = parts[1].trim();

            int fkId = po.get_ValueAsInt(fkCol);
            if (fkId <= 0) return null;

            // Derive nama tabel: C_BPartner_ID → C_BPartner
            String fkTableName = fkCol.endsWith("_ID")
                    ? fkCol.substring(0, fkCol.length() - 3)
                    : fkCol;

            try {
                MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
                if (fkTable == null) {
                    log.warning("[WFDetail] FK table tidak ditemukan: " + fkTableName);
                    return null;
                }
                PO fkPO = fkTable.getPO(fkId, null);
                if (fkPO == null) return null;

                Object val = fkPO.get_Value(targetCol);
                return val != null ? val.toString().trim() : null;

            } catch (Exception e) {
                log.warning("[WFDetail] FK lookup gagal [" + columnDef + "]: " + e.getMessage());
                return null;
            }

        } else {
            // Kolom biasa
            Object val = po.get_Value(columnDef);
            return val != null ? val.toString().trim() : null;
        }
    }

    // =========================================================================
    // PRIVATE — NUMERIC HANDLING (v2.0)
    // =========================================================================

    /**
     * Sanitize nilai qty/amt yang mungkin berupa string non-numerik.
     *
     * - Null / kosong / whitespace → return "0"
     * - Nilai numerik valid        → return apa adanya (tanpa modifikasi)
     * - Nilai non-numerik (string) → return "0" + log warning
     *
     * @param raw       nilai mentah dari PO
     * @param context   label untuk keperluan logging (misal "QTY_COL[C_Order]")
     */
    private String sanitizeNumeric(String raw, String context) {
        if (isEmpty(raw) || "-".equals(raw)) return "0";

        if (isNumericString(raw)) return raw;

        // Nilai ada tapi bukan angka — kemungkinan salah konfigurasi kolom
        log.warning("[WFDetail] " + context
                + ": nilai '" + raw + "' bukan numerik, ditampilkan sebagai '0'."
                + " Periksa konfigurasi QTY_COL/AMT_COL di System Configurator.");
        return "0";
    }

    /**
     * Cek apakah string adalah angka valid (termasuk desimal dan negatif).
     * Contoh valid   : "100", "1500.50", "-200.00", "0"
     * Contoh invalid : "N/A", "---", "Approved", ""
     */
    private boolean isNumericString(String value) {
        if (isEmpty(value)) return false;
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Format amount ke string dengan separator ribuan sesuai locale iDempiere.
     * Jika nilai bukan numerik (seharusnya sudah disanitize sebelumnya),
     * log warning dan return nilai mentah.
     *
     * @param rawValue  nilai string yang diharapkan numerik
     * @param context   label untuk logging
     */
    private String formatAmount(String rawValue, String context) {
        if (isEmpty(rawValue) || "-".equals(rawValue)) return "-";

        try {
            BigDecimal bd = new BigDecimal(rawValue);
            NumberFormat nf = NumberFormat.getNumberInstance(
                    Env.getLanguage(Env.getCtx()).getLocale());
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(bd);
        } catch (NumberFormatException e) {
            log.warning("[WFDetail] formatAmount gagal untuk " + context
                    + ": nilai='" + rawValue + "' bukan numerik, ditampilkan apa adanya.");
            return rawValue;
        }
    }

    // =========================================================================
    // PRIVATE — DATE FORMATTING
    // =========================================================================

    /**
     * Format tanggal ke "dd MMM yyyy" sesuai locale iDempiere.
     * Handle Timestamp, java.sql.Date, dan java.util.Date.
     */
    private String formatDate(Object dateObj) {
        if (dateObj == null) return "-";
        try {
            java.util.Date date;
            if (dateObj instanceof java.sql.Timestamp)
                date = new java.util.Date(((java.sql.Timestamp) dateObj).getTime());
            else if (dateObj instanceof java.sql.Date)
                date = new java.util.Date(((java.sql.Date) dateObj).getTime());
            else if (dateObj instanceof java.util.Date)
                date = (java.util.Date) dateObj;
            else
                return dateObj.toString();

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy",
                    Env.getLanguage(Env.getCtx()).getLocale());
            return sdf.format(date);

        } catch (Exception e) {
            log.warning("[WFDetail] formatDate gagal: " + e.getMessage());
            return dateObj.toString();
        }
    }

    // =========================================================================
    // PRIVATE — BP FALLBACK
    // =========================================================================

    /**
     * Fallback jika kolom BP tidak ditemukan:
     * tampilkan nama user yang membuat dokumen (CreatedBy).
     */
    private String resolveCreatedByName(PO headerPO) {
        try {
            MTable userTable = MTable.get(Env.getCtx(), "AD_User");
            PO userPO = userTable.getPO(headerPO.getCreatedBy(), null);
            if (userPO != null) {
                Object name = userPO.get_Value("Name");
                if (name != null && !name.toString().trim().isEmpty())
                    return "By: " + name.toString().trim();
            }
        } catch (Exception e) {
            log.warning("[WFDetail] resolveCreatedByName gagal: " + e.getMessage());
        }
        return "-";
    }

    // =========================================================================
    // PRIVATE — UI UTILITIES
    // =========================================================================

    /**
     * Reset semua UI component ke kondisi awal / kosong.
     * Hapus hanya Listitem — Listhead dibiarkan agar header kolom tetap tampil.
     */
    private void clearUI() {
        List<Listitem> toRemove = new ArrayList<>(lstTxLines.getItems());
        for (Listitem item : toRemove)
            lstTxLines.removeChild(item);

        lHdrDocNo.setValue("-");
        lHdrDateDoc.setValue("-");
        lHdrBPName.setValue("-");
        lHdrGrandTotal.setValue("-");
        //grpTxDetails.setCaption("Detail Transaksi");
        org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption("Detail Transaksi");
        grpTxDetails.appendChild(cp);
        grpTxDetails.setVisible(false);
    }

    /**
     * Tambahkan satu baris informasi/pesan di Listbox lines.
     * Dipakai untuk kondisi kosong, error, atau warning konfigurasi.
     */
    private void appendInfoRow(String message) {
        Listitem item = new Listitem();
        item.appendChild(new Listcell(message));
        item.appendChild(new Listcell("-"));
        item.appendChild(new Listcell("-"));
        lstTxLines.appendChild(item);
    }

    /**
     * Null-safe empty check — true jika null, kosong, atau hanya whitespace.
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
