/*******************************************************************************
 * WFTransactionDetailRenderer.java
 *
 * Renderer class untuk menampilkan detail dokumen transaksi
 * pada form Workflow Approval (WWFActivity).
 *
 * Arsitektur: SysConfig-driven + Generic PO + SQL Query
 * Tidak ada dependency ke model spesifik (MOrder, MInvoice, dll)
 *
 * Konfigurasi via System Configurator iDempiere:
 * ─────────────────────────────────────────────────────────────────────────────
 * KEY PATTERN : WF_DETAIL_<TableName><Suffix>
 * ─────────────────────────────────────────────────────────────────────────────
 * Suffix            Keterangan                          Contoh Value
 * _LINE_TABLE       Nama tabel line                     C_OrderLine
 * _LINK_COL         FK kolom di tabel line              C_Order_ID
 * _DESC_COL         Kolom deskripsi/produk (bisa FK)    M_Product_ID>Name,Description
 * _QTY_COL          Kolom qty (dicoba berurutan)         QtyOrdered,QtyEntered
 * _AMT_COL          Kolom amount (dicoba berurutan)      LineNetAmt,PriceActual
 * _HDR_DOCNO        Kolom nomor dokumen                 DocumentNo
 * _HDR_BP           Kolom business partner (bisa FK)    C_BPartner_ID>Name
 * _HDR_DATE         Kolom tanggal (dicoba berurutan)    DateOrdered,DateDoc
 * _HDR_TOTAL        Kolom grand total                   GrandTotal,TotalLines
 * ─────────────────────────────────────────────────────────────────────────────
 * Gunakan "-" sebagai value jika kolom tidak relevan untuk tabel tersebut.
 * Jika SysConfig tidak ada, renderer tetap tampil dengan fallback generik.
 *
 * Contoh Konfigurasi:
 * ─────────────────────────────────────────────────────────────────────────────
 * C_Order:
 *   WF_DETAIL_C_Order_LINE_TABLE  = C_OrderLine
 *   WF_DETAIL_C_Order_LINK_COL   = C_Order_ID
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
 *   WF_DETAIL_M_Requisition_DESC_COL   = M_Product_ID>Name,Description
 *   WF_DETAIL_M_Requisition_QTY_COL    = Qty
 *   WF_DETAIL_M_Requisition_AMT_COL    = LineNetAmt
 *   WF_DETAIL_M_Requisition_HDR_DOCNO  = DocumentNo
 *   WF_DETAIL_M_Requisition_HDR_BP     = C_BPartner_ID>Name
 *   WF_DETAIL_M_Requisition_HDR_DATE   = DateRequired,DateDoc
 *   WF_DETAIL_M_Requisition_HDR_TOTAL  = TotalLines
 * ─────────────────────────────────────────────────────────────────────────────
 ******************************************************************************/
package org.adempiere.webui.apps.wf;

import java.math.BigDecimal;
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

public class WFTransactionDetailRenderer {

    // =========================================================================
    // LOGGER
    // =========================================================================
    private static final CLogger log = CLogger.getCLogger(WFTransactionDetailRenderer.class);

    // =========================================================================
    // SYSCONFIG KEY CONSTANTS
    // =========================================================================
    private static final String PREFIX       = "WF_DETAIL_";
    private static final String LINE_TABLE   = "_LINE_TABLE";
    private static final String LINK_COL     = "_LINK_COL";
    private static final String DESC_COL     = "_DESC_COL";
    private static final String QTY_COL      = "_QTY_COL";
    private static final String AMT_COL      = "_AMT_COL";
    private static final String HDR_DOCNO    = "_HDR_DOCNO";
    private static final String HDR_BP       = "_HDR_BP";
    private static final String HDR_DATE     = "_HDR_DATE";
    private static final String HDR_TOTAL    = "_HDR_TOTAL";

    // =========================================================================
    // DEFAULT FALLBACK VALUES
    // Dipakai jika SysConfig tidak dikonfigurasi untuk tabel tertentu
    // =========================================================================
    private static final String DEFAULT_DOCNO   = "DocumentNo,Value,Name";
    private static final String DEFAULT_BP      = "C_BPartner_ID>Name";
    private static final String DEFAULT_DATE    = "DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created";
    private static final String DEFAULT_TOTAL   = "GrandTotal,TotalLines,-";
    private static final String DEFAULT_DESC    = "M_Product_ID>Name,Description,Name";
    private static final String DEFAULT_QTY     = "QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty";
    private static final String DEFAULT_AMT     = "LineNetAmt,PriceActual,-";

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
    // Semua UI component disuntikkan dari luar (tidak ada coupling ke WWFActivity)
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
    // PUBLIC API — dipanggil dari WWFActivity.display()
    // =========================================================================

    /**
     * Entry point utama. Dipanggil dari WWFActivity setiap kali
     * item approval dipilih di listbox kiri.
     *
     * @param activity MWFActivity yang sedang aktif, boleh null
     */
    public void render(MWFActivity activity) {
        clearUI();

        if (activity == null || activity.getRecord_ID() <= 0) {
            grpTxDetails.setVisible(false);
            return;
        }

        try {
            int clientId = activity.getAD_Client_ID();
            int tableId  = activity.getAD_Table_ID();
            int recordId = activity.getRecord_ID();

            MTable table     = MTable.get(Env.getCtx(), tableId);
            String tableName = table.getTableName();

            PO headerPO = table.getPO(recordId, null);
            if (headerPO == null) {
                grpTxDetails.setCaption("Detail (" + tableName + " #" + recordId + " tidak ditemukan)");
                grpTxDetails.setVisible(true);
                return;
            }

            renderHeader(headerPO, tableName, clientId);
            renderLines(headerPO, tableName, recordId, clientId);

            grpTxDetails.setVisible(true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "WFTransactionDetailRenderer.render() gagal", e);
            // Form tidak crash — tampilkan pesan error ringan di caption groupbox
            grpTxDetails.setCaption("Detail (Error: " + e.getMessage() + ")");
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
        String dateCfg  = getSysConfig(tableName, HDR_DATE, clientId, DEFAULT_DATE);
        Object rawDate  = resolveColumnObject(headerPO, dateCfg);
        lHdrDateDoc.setValue(rawDate != null ? formatDate(rawDate) : "-");

        // Business Partner
        String bpCfg    = getSysConfig(tableName, HDR_BP, clientId, DEFAULT_BP);
        String bpName   = resolveColumnValue(headerPO, bpCfg);
        if (isEmpty(bpName))
            bpName = resolveCreatedByName(headerPO);
        lHdrBPName.setValue(bpName);

        // Grand Total
        String totalCfg = getSysConfig(tableName, HDR_TOTAL, clientId, DEFAULT_TOTAL);
        String total    = resolveColumnValue(headerPO, totalCfg);
        lHdrGrandTotal.setValue(isEmpty(total) ? "-" : formatAmount(total));

        // Update caption groupbox
        grpTxDetails.setCaption("Detail: " + tableName + "  #" + headerPO.get_ID());
    }

    // =========================================================================
    // PRIVATE — RENDER LINES
    // =========================================================================

    private void renderLines(PO headerPO, String tableName, int recordId, int clientId) {

        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        String linkCol   = getSysConfig(tableName, LINK_COL,   clientId, null);

        // Jika tidak dikonfigurasi atau eksplisit "-", sembunyikan section lines
        if (isEmpty(lineTable) || isEmpty(linkCol)
                || "-".equals(lineTable) || "-".equals(linkCol)) {
            lstTxLines.setVisible(false);
            return;
        }

        String descCfg  = getSysConfig(tableName, DESC_COL, clientId, DEFAULT_DESC);
        String qtyCfg   = getSysConfig(tableName, QTY_COL,  clientId, DEFAULT_QTY);
        String amtCfg   = getSysConfig(tableName, AMT_COL,  clientId, DEFAULT_AMT);

        lstTxLines.setVisible(true);

        try {
            List<PO> lines = new Query(Env.getCtx(), lineTable,
                    linkCol + " = ?", null)
                    .setParameters(recordId)
                    .setOrderBy("Line, " + linkCol)
                    .list();

            if (lines == null || lines.isEmpty()) {
                appendEmptyRow("(Tidak ada baris detail)");
                return;
            }

            for (PO line : lines) {
                String desc = resolveColumnValue(line, descCfg);
                if (isEmpty(desc)) desc = "Item #" + line.get_ID();

                String qty  = resolveColumnValue(line, qtyCfg);
                if (isEmpty(qty) || "-".equals(qty)) qty = "0";

                String amt  = resolveColumnValue(line, amtCfg);
                if (isEmpty(amt) || "-".equals(amt)) amt = "-";
                else amt = formatAmount(amt);

                Listitem item = new Listitem();
                item.appendChild(new Listcell(desc));
                item.appendChild(new Listcell(qty));
                item.appendChild(new Listcell(amt));
                lstTxLines.appendChild(item);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "renderLines gagal: lineTable=" + lineTable
                    + ", linkCol=" + linkCol, e);
            appendEmptyRow("Error memuat baris: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE — SYSCONFIG READER
    // =========================================================================

    /**
     * Baca SysConfig dengan key pattern: WF_DETAIL_<TableName><suffix>
     * Contoh: WF_DETAIL_C_Order_LINE_TABLE
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
     * Dicoba satu per satu, return nilai pertama yang tidak kosong.
     *
     * Mendukung notasi FK lookup: "C_BPartner_ID>Name"
     * Untuk skip kolom: gunakan "-"
     */
    private String resolveColumnValue(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : columnDefs.split(",")) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;

            String value = resolveOneColumn(po, candidate);
            if (!isEmpty(value)) return value;
        }
        return null;
    }

    /**
     * Resolve Object asli dari kolom (tidak melalui FK).
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
     * Format biasa  : "DocumentNo"
     * Format FK     : "C_BPartner_ID>Name"
     *
     * Untuk FK lookup, nama tabel di-derive dari nama kolom:
     *   C_BPartner_ID → tabel C_BPartner
     *   M_Product_ID  → tabel M_Product
     */
    private String resolveOneColumn(PO po, String columnDef) {
        if (columnDef == null || columnDef.isEmpty()) return null;

        if (columnDef.contains(">")) {
            // FK Lookup
            String[] parts = columnDef.split(">", 2);
            if (parts.length != 2) return null;

            String fkCol     = parts[0].trim(); // contoh: C_BPartner_ID
            String targetCol = parts[1].trim(); // contoh: Name

            int fkId = po.get_ValueAsInt(fkCol);
            if (fkId <= 0) return null;

            // Derive nama tabel dari nama kolom FK
            // C_BPartner_ID → C_BPartner | M_Product_ID → M_Product
            String fkTableName = fkCol.endsWith("_ID")
                    ? fkCol.substring(0, fkCol.length() - 3)
                    : fkCol;

            try {
                MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
                if (fkTable == null) {
                    log.warning("FK table tidak ditemukan: " + fkTableName);
                    return null;
                }

                PO fkPO = fkTable.getPO(fkId, null);
                if (fkPO == null) return null;

                Object val = fkPO.get_Value(targetCol);
                return val != null ? val.toString() : null;

            } catch (Exception e) {
                log.warning("FK lookup gagal [" + columnDef + "]: " + e.getMessage());
                return null;
            }

        } else {
            // Kolom biasa
            Object val = po.get_Value(columnDef);
            return val != null ? val.toString() : null;
        }
    }

    // =========================================================================
    // PRIVATE — UTILITY METHODS
    // =========================================================================

    /**
     * Fallback untuk BP: tampilkan nama user yang membuat dokumen
     */
    private String resolveCreatedByName(PO headerPO) {
        try {
            int createdBy = headerPO.getCreatedBy();
            MTable userTable = MTable.get(Env.getCtx(), "AD_User");
            PO userPO = userTable.getPO(createdBy, null);
            if (userPO != null) {
                Object name = userPO.get_Value("Name");
                if (name != null && !name.toString().trim().isEmpty())
                    return "By: " + name.toString();
            }
        } catch (Exception e) {
            log.warning("resolveCreatedByName gagal: " + e.getMessage());
        }
        return "-";
    }

    /**
     * Format tanggal ke "dd MMM yyyy" sesuai locale iDempiere
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

            org.compiere.util.Language lang = Env.getLanguage(Env.getCtx());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", lang.getLocale());
            return sdf.format(date);

        } catch (Exception e) {
            return dateObj.toString();
        }
    }

    /**
     * Format amount: tambahkan separator ribuan jika string adalah angka
     */
    private String formatAmount(String rawValue) {
        if (rawValue == null || rawValue.isEmpty() || "-".equals(rawValue))
            return rawValue;
        try {
            BigDecimal bd = new BigDecimal(rawValue);
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(
                    Env.getLanguage(Env.getCtx()).getLocale());
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(bd);
        } catch (Exception ignored) {
            return rawValue; // return apa adanya jika bukan angka
        }
    }

    /**
     * Reset semua UI component ke kondisi kosong
     */
    private void clearUI() {
        // Hapus hanya Listitem — biarkan Listhead tetap
        List<Listitem> toRemove = new ArrayList<>(lstTxLines.getItems());
        for (Listitem item : toRemove)
            lstTxLines.removeChild(item);

        lHdrDocNo.setValue("-");
        lHdrDateDoc.setValue("-");
        lHdrBPName.setValue("-");
        lHdrGrandTotal.setValue("-");
        grpTxDetails.setCaption("Detail Transaksi");
        grpTxDetails.setVisible(false);
    }

    /**
     * Tambahkan satu baris placeholder (kosong / error message)
     */
    private void appendEmptyRow(String message) {
        Listitem item = new Listitem();
        item.appendChild(new Listcell(message));
        item.appendChild(new Listcell("-"));
        item.appendChild(new Listcell("-"));
        lstTxLines.appendChild(item);
    }

    /**
     * Null-safe empty check
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
