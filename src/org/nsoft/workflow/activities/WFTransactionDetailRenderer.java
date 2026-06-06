/*******************************************************************************
 * WFTransactionDetailRenderer.java
 *
 * Renderer class to query document details
 * at Workflow Approval (WWFActivity) Form
 *
 * Architecture : SysConfig-driven + Generic PO + SQL Query
 * Versi        : 3.0 — COL1/COL2/COL3 + LABEL dinamis, type-aware formatting,
 *                multi-level FK lookup, flexible column header
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * SYSCONFIG KEY PATTERN : WF_DETAIL_<TableName><Suffix>
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * === Lines (Document Details) ===
 *
 * Suffix           Mandatory   Descriptiion                         Sample Value
 *___________________________________________________________________________________________
 * _LINE_TABLE      N           Table Line Name                      C_OrderLine
 * _LINK_COL        N*          FK Column in table line              C_Order_ID
 * _ORDER_BY        N           Order by query lines                 Line,C_Order_ID
 *
 * _COL1            N           First Column (desk/product)          M_Product_ID>Name,Description
 * _COL1_LABEL      N           Label header first column            Product / Description
 * _COL1_TYPE       N           Column Type(numeric/string)          string  [default: string]
 *
 * _COL2            N           Second column (qty or other field)   QtyOrdered,QtyEntered
 * _COL2_LABEL      N           Label header 2nd column              Qty / Kota
 * _COL2_TYPE       N           Column Type(numeric/string)          numeric [default: numeric]
 *
 * _COL3            N           Third Column(amount/other field)     LineNetAmt
 * _COL3_LABEL      N           Label header 3th Column              Amount / Address
 * _COL3_TYPE       N           Column Type(numeric/string)          numeric [default: numeric]
 *
 * === HEADER ===
 *
 * _HDR_COL1        N       Header West (doc number)             DocumentNo,Value,Name
 * _HDR_COL1_LABEL  N       Label West header                    No. Document
 * _HDR_COL2        N       Header tengah-kiri (BP/pihak)        C_BPartner_ID>Name
 * _HDR_COL2_LABEL  N       Label header tengah-kiri             Business Partner
 * _HDR_COL3        N       Header tengah-kanan (tanggal)        DateOrdered,DateDoc
 * _HDR_COL3_LABEL  N       Label header tengah-kanan            Tanggal
 * _HDR_COL4        N       Header kanan (total/info lain)       GrandTotal
 * _HDR_COL4_LABEL  N       Label header kanan                   Grand Total
 * _HDR_COL4_TYPE   N       Tipe header kanan (numeric/string)   numeric [default: numeric]
 *
 * *) LINK_COL wajib jika LINE_TABLE diisi
 *
 * Gunakan "-" sebagai value untuk menonaktifkan kolom tertentu.
 * Jika SysConfig tidak dikonfigurasi sama sekali, renderer tetap
 * tampil dengan nilai fallback generik — tidak crash.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CONTOH KONFIGURASI
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * C_Order (Purchase/Sales Order):
 *   WF_DETAIL_C_Order_LINE_TABLE   = C_OrderLine
 *   WF_DETAIL_C_Order_LINK_COL    = C_Order_ID
 *   WF_DETAIL_C_Order_ORDER_BY    = Line
 *   WF_DETAIL_C_Order_COL1        = M_Product_ID>Name,Description
 *   WF_DETAIL_C_Order_COL1_LABEL  = Produk / Deskripsi
 *   WF_DETAIL_C_Order_COL2        = QtyOrdered,QtyEntered
 *   WF_DETAIL_C_Order_COL2_LABEL  = Qty
 *   WF_DETAIL_C_Order_COL2_TYPE   = numeric
 *   WF_DETAIL_C_Order_COL3        = LineNetAmt
 *   WF_DETAIL_C_Order_COL3_LABEL  = Net Amount
 *   WF_DETAIL_C_Order_COL3_TYPE   = numeric
 *   WF_DETAIL_C_Order_HDR_COL1       = DocumentNo
 *   WF_DETAIL_C_Order_HDR_COL1_LABEL = No. Order
 *   WF_DETAIL_C_Order_HDR_COL2       = C_BPartner_ID>Name
 *   WF_DETAIL_C_Order_HDR_COL2_LABEL = Vendor / Customer
 *   WF_DETAIL_C_Order_HDR_COL3       = DateOrdered,DateDoc
 *   WF_DETAIL_C_Order_HDR_COL3_LABEL = Tanggal Order
 *   WF_DETAIL_C_Order_HDR_COL4       = GrandTotal
 *   WF_DETAIL_C_Order_HDR_COL4_LABEL = Grand Total
 *   WF_DETAIL_C_Order_HDR_COL4_TYPE  = numeric
 *
 * C_Invoice:
 *   WF_DETAIL_C_Invoice_LINE_TABLE   = C_InvoiceLine
 *   WF_DETAIL_C_Invoice_LINK_COL    = C_Invoice_ID
 *   WF_DETAIL_C_Invoice_ORDER_BY    = Line
 *   WF_DETAIL_C_Invoice_COL1        = M_Product_ID>Name,Description
 *   WF_DETAIL_C_Invoice_COL1_LABEL  = Produk / Deskripsi
 *   WF_DETAIL_C_Invoice_COL2        = QtyInvoiced
 *   WF_DETAIL_C_Invoice_COL2_LABEL  = Qty Invoice
 *   WF_DETAIL_C_Invoice_COL2_TYPE   = numeric
 *   WF_DETAIL_C_Invoice_COL3        = LineNetAmt
 *   WF_DETAIL_C_Invoice_COL3_LABEL  = Net Amount
 *   WF_DETAIL_C_Invoice_COL3_TYPE   = numeric
 *   WF_DETAIL_C_Invoice_HDR_COL1       = DocumentNo
 *   WF_DETAIL_C_Invoice_HDR_COL1_LABEL = No. Invoice
 *   WF_DETAIL_C_Invoice_HDR_COL2       = C_BPartner_ID>Name
 *   WF_DETAIL_C_Invoice_HDR_COL2_LABEL = Vendor / Customer
 *   WF_DETAIL_C_Invoice_HDR_COL3       = DateInvoiced,DateDoc
 *   WF_DETAIL_C_Invoice_HDR_COL3_LABEL = Tanggal Invoice
 *   WF_DETAIL_C_Invoice_HDR_COL4       = GrandTotal
 *   WF_DETAIL_C_Invoice_HDR_COL4_LABEL = Grand Total
 *   WF_DETAIL_C_Invoice_HDR_COL4_TYPE  = numeric
 *
 * M_InOut (Shipment / Receipt):
 *   WF_DETAIL_M_InOut_LINE_TABLE   = M_InOutLine
 *   WF_DETAIL_M_InOut_LINK_COL    = M_InOut_ID
 *   WF_DETAIL_M_InOut_ORDER_BY    = Line
 *   WF_DETAIL_M_InOut_COL1        = M_Product_ID>Name,Description
 *   WF_DETAIL_M_InOut_COL1_LABEL  = Produk
 *   WF_DETAIL_M_InOut_COL2        = MovementQty,QtyEntered
 *   WF_DETAIL_M_InOut_COL2_LABEL  = Qty Movement
 *   WF_DETAIL_M_InOut_COL2_TYPE   = numeric
 *   WF_DETAIL_M_InOut_COL3        = -
 *   WF_DETAIL_M_InOut_COL3_LABEL  = -
 *   WF_DETAIL_M_InOut_HDR_COL1       = DocumentNo
 *   WF_DETAIL_M_InOut_HDR_COL1_LABEL = No. Dokumen
 *   WF_DETAIL_M_InOut_HDR_COL2       = C_BPartner_ID>Name
 *   WF_DETAIL_M_InOut_HDR_COL2_LABEL = Vendor / Customer
 *   WF_DETAIL_M_InOut_HDR_COL3       = MovementDate,DateDoc
 *   WF_DETAIL_M_InOut_HDR_COL3_LABEL = Tanggal Pengiriman
 *   WF_DETAIL_M_InOut_HDR_COL4       = -
 *   WF_DETAIL_M_InOut_HDR_COL4_LABEL = -
 *
 * M_Requisition:
 *   WF_DETAIL_M_Requisition_LINE_TABLE   = M_RequisitionLine
 *   WF_DETAIL_M_Requisition_LINK_COL    = M_Requisition_ID
 *   WF_DETAIL_M_Requisition_ORDER_BY    = Line
 *   WF_DETAIL_M_Requisition_COL1        = M_Product_ID>Name,Description
 *   WF_DETAIL_M_Requisition_COL1_LABEL  = Produk / Deskripsi
 *   WF_DETAIL_M_Requisition_COL2        = Qty
 *   WF_DETAIL_M_Requisition_COL2_LABEL  = Qty
 *   WF_DETAIL_M_Requisition_COL2_TYPE   = numeric
 *   WF_DETAIL_M_Requisition_COL3        = LineNetAmt
 *   WF_DETAIL_M_Requisition_COL3_LABEL  = Net Amount
 *   WF_DETAIL_M_Requisition_COL3_TYPE   = numeric
 *   WF_DETAIL_M_Requisition_HDR_COL1       = DocumentNo
 *   WF_DETAIL_M_Requisition_HDR_COL1_LABEL = No. Requisisi
 *   WF_DETAIL_M_Requisition_HDR_COL2       = C_BPartner_ID>Name
 *   WF_DETAIL_M_Requisition_HDR_COL2_LABEL = Vendor
 *   WF_DETAIL_M_Requisition_HDR_COL3       = DateRequired,DateDoc
 *   WF_DETAIL_M_Requisition_HDR_COL3_LABEL = Tanggal Dibutuhkan
 *   WF_DETAIL_M_Requisition_HDR_COL4       = TotalLines
 *   WF_DETAIL_M_Requisition_HDR_COL4_LABEL = Total
 *   WF_DETAIL_M_Requisition_HDR_COL4_TYPE  = numeric
 *
 * C_BPartner (Approval tambah BP baru — tanpa lines, pakai lokasi sebagai lines):
 *   WF_DETAIL_C_BPartner_LINE_TABLE   = C_BPartner_Location
 *   WF_DETAIL_C_BPartner_LINK_COL    = C_BPartner_ID
 *   WF_DETAIL_C_BPartner_ORDER_BY    = C_BPartner_ID
 *   WF_DETAIL_C_BPartner_COL1        = C_Location_ID>Address1,Name
 *   WF_DETAIL_C_BPartner_COL1_LABEL  = Alamat
 *   WF_DETAIL_C_BPartner_COL2        = C_Location_ID>City
 *   WF_DETAIL_C_BPartner_COL2_LABEL  = Kota
 *   WF_DETAIL_C_BPartner_COL2_TYPE   = string
 *   WF_DETAIL_C_BPartner_COL3        = C_Location_ID>Postal
 *   WF_DETAIL_C_BPartner_COL3_LABEL  = Kode Pos
 *   WF_DETAIL_C_BPartner_COL3_TYPE   = string
 *   WF_DETAIL_C_BPartner_HDR_COL1       = Value,Name
 *   WF_DETAIL_C_BPartner_HDR_COL1_LABEL = Kode BP
 *   WF_DETAIL_C_BPartner_HDR_COL2       = C_BP_Group_ID>Name
 *   WF_DETAIL_C_BPartner_HDR_COL2_LABEL = Grup BP
 *   WF_DETAIL_C_BPartner_HDR_COL3       = Created
 *   WF_DETAIL_C_BPartner_HDR_COL3_LABEL = Tgl. Dibuat
 *   WF_DETAIL_C_BPartner_HDR_COL4       = -
 *   WF_DETAIL_C_BPartner_HDR_COL4_LABEL = -
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CHANGELOG
 * ─────────────────────────────────────────────────────────────────────────────
 * v3.0 - Ganti _QTY_COL/_AMT_COL/_DESC_COL menjadi _COL1/_COL2/_COL3
 *      - Tambah _COL1_LABEL/_COL2_LABEL/_COL3_LABEL — header listbox dinamis
 *      - Tambah _COL1_TYPE/_COL2_TYPE/_COL3_TYPE (default: string/numeric/numeric)
 *      - Ganti _HDR_DOCNO/_HDR_BP/_HDR_DATE/_HDR_TOTAL menjadi _HDR_COL1..COL4
 *      - Tambah _HDR_COL1_LABEL.._HDR_COL4_LABEL — label header panel dinamis
 *      - Tambah _HDR_COL4_TYPE untuk kontrol format header kanan
 *      - Tambah renderListhead() — rebuild Listhead dari konfigurasi
 *      - resolveOneColumn() mendukung multi-level FK chain (N level)
 *      - formatByType() terpusat menggantikan sanitizeNumeric()
 *      - Backward compatibility: fallback default tetap generik jika tidak dikonfigurasi
 * v2.0 - Fix ORDER_BY configurable, validateAndWarnConfig(), isNumericValue(),
 *        sanitizeNumeric(), formatAmount() log warning
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
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;


public class WFTransactionDetailRenderer {

    // =========================================================================
    // LOGGER
    // =========================================================================
    private static final CLogger log = CLogger.getCLogger(WFTransactionDetailRenderer.class);

    // =========================================================================
    // SYSCONFIG KEY CONSTANTS
    // =========================================================================
    private static final String PREFIX         = "WF_DETAIL_";

    // --- Line table ---
    private static final String LINE_TABLE     = "_LINE_TABLE";
    private static final String LINK_COL       = "_LINK_COL";
    private static final String ORDER_BY       = "_ORDER_BY";

    // --- Line columns (v3.0: COL1/COL2/COL3 + LABEL + TYPE) ---
    private static final String COL1           = "_COL1";
    private static final String COL1_LABEL     = "_COL1_LABEL";
    private static final String COL1_TYPE      = "_COL1_TYPE";

    private static final String COL2           = "_COL2";
    private static final String COL2_LABEL     = "_COL2_LABEL";
    private static final String COL2_TYPE      = "_COL2_TYPE";

    private static final String COL3           = "_COL3";
    private static final String COL3_LABEL     = "_COL3_LABEL";
    private static final String COL3_TYPE      = "_COL3_TYPE";

    // --- Header fields (v3.0: HDR_COL1..COL4 + LABEL + TYPE) ---
    private static final String HDR_COL1       = "_HDR_COL1";
    private static final String HDR_COL1_LABEL = "_HDR_COL1_LABEL";

    private static final String HDR_COL2       = "_HDR_COL2";
    private static final String HDR_COL2_LABEL = "_HDR_COL2_LABEL";

    private static final String HDR_COL3       = "_HDR_COL3";
    private static final String HDR_COL3_LABEL = "_HDR_COL3_LABEL";

    private static final String HDR_COL4       = "_HDR_COL4";
    private static final String HDR_COL4_LABEL = "_HDR_COL4_LABEL";
    private static final String HDR_COL4_TYPE  = "_HDR_COL4_TYPE";

    // =========================================================================
    // TYPE CONSTANTS
    // =========================================================================
    private static final String TYPE_NUMERIC   = "numeric";
    private static final String TYPE_STRING    = "string";

    // =========================================================================
    // DEFAULT FALLBACK VALUES
    // Dipakai jika SysConfig tidak dikonfigurasi untuk tabel tertentu.
    // Urutan dicoba dari kiri ke kanan — berhenti di yang pertama ada.
    // =========================================================================
    private static final String DEFAULT_COL1       = "M_Product_ID>Name,Description,Name";
    private static final String DEFAULT_COL1_LABEL = "Deskripsi";
    private static final String DEFAULT_COL1_TYPE  = TYPE_STRING;   // deskripsi selalu string

    private static final String DEFAULT_COL2       = "QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty";
    private static final String DEFAULT_COL2_LABEL = "Qty";
    private static final String DEFAULT_COL2_TYPE  = TYPE_NUMERIC;

    private static final String DEFAULT_COL3       = "LineNetAmt,PriceActual,-";
    private static final String DEFAULT_COL3_LABEL = "Amount";
    private static final String DEFAULT_COL3_TYPE  = TYPE_NUMERIC;

    private static final String DEFAULT_HDR_COL1       = "DocumentNo,Value,Name";
    private static final String DEFAULT_HDR_COL1_LABEL = "No. Dokumen";

    private static final String DEFAULT_HDR_COL2       = "C_BPartner_ID>Name";
    private static final String DEFAULT_HDR_COL2_LABEL = "Business Partner";

    private static final String DEFAULT_HDR_COL3       = "DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created";
    private static final String DEFAULT_HDR_COL3_LABEL = "Tanggal";

    private static final String DEFAULT_HDR_COL4       = "GrandTotal,TotalLines,-";
    private static final String DEFAULT_HDR_COL4_LABEL = "Total";
    private static final String DEFAULT_HDR_COL4_TYPE  = TYPE_NUMERIC;

    // =========================================================================
    // UI COMPONENTS — disuntikkan dari WWFActivity via constructor
    // =========================================================================
    private final Groupbox grpTxDetails;
    private final Listbox  lstTxLines;
    private final Label    lHdrCol1;    // ex lHdrDocNo
    private final Label    lHdrCol2;    // ex lHdrBPName
    private final Label    lHdrCol3;    // ex lHdrDateDoc
    private final Label    lHdrCol4;    // ex lHdrGrandTotal

    // Label untuk judul field di panel header (opsional — null jika tidak ada di UI)
    private final Label    lHdrCol1Title;
    private final Label    lHdrCol2Title;
    private final Label    lHdrCol3Title;
    private final Label    lHdrCol4Title;

    // =========================================================================
    // CONSTRUCTOR — minimal (tanpa title labels)
    // =========================================================================
    public WFTransactionDetailRenderer(
            Groupbox grpTxDetails,
            Listbox  lstTxLines,
            Label    lHdrCol1,
            Label    lHdrCol2,
            Label    lHdrCol3,
            Label    lHdrCol4) {

        this(grpTxDetails, lstTxLines,
                lHdrCol1, null,
                lHdrCol2, null,
                lHdrCol3, null,
                lHdrCol4, null);
    }

    // =========================================================================
    // CONSTRUCTOR — lengkap (dengan title labels)
    // Gunakan ini agar label judul di panel header juga ikut berubah dinamis.
    // =========================================================================
    public WFTransactionDetailRenderer(
            Groupbox grpTxDetails,
            Listbox  lstTxLines,
            Label    lHdrCol1,   Label lHdrCol1Title,
            Label    lHdrCol2,   Label lHdrCol2Title,
            Label    lHdrCol3,   Label lHdrCol3Title,
            Label    lHdrCol4,   Label lHdrCol4Title) {

        this.grpTxDetails   = grpTxDetails;
        this.lstTxLines     = lstTxLines;
        this.lHdrCol1       = lHdrCol1;
        this.lHdrCol1Title  = lHdrCol1Title;
        this.lHdrCol2       = lHdrCol2;
        this.lHdrCol2Title  = lHdrCol2Title;
        this.lHdrCol3       = lHdrCol3;
        this.lHdrCol3Title  = lHdrCol3Title;
        this.lHdrCol4       = lHdrCol4;
        this.lHdrCol4Title  = lHdrCol4Title;
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

            validateAndWarnConfig(tableName, clientId);

            PO headerPO = table.getPO(recordId, null);
            if (headerPO == null) {
                org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption(
                        "Detail (" + tableName + " #" + recordId + " tidak ditemukan)");
                grpTxDetails.appendChild(cp);
                grpTxDetails.setVisible(true);
                return;
            }

            renderHeader(headerPO, tableName, clientId);
            renderLines(headerPO, tableName, recordId, clientId);

            grpTxDetails.setVisible(true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "WFTransactionDetailRenderer.render() gagal", e);
            setGroupboxCaption("Detail (" + tableName + " #" + recordId + " tidak ditemukan)");
            setGroupboxCaption("Detail (Error: " + e.getMessage() + ")");
        }
    }


    private void renderHeader(PO headerPO, String tableName, int clientId) {

        // HDR_COL1 — No. Dokumen / Kode / Value
        String hdr1Cfg   = getSysConfig(tableName, HDR_COL1, clientId, DEFAULT_HDR_COL1);
        String hdr1Label = getSysConfig(tableName, HDR_COL1_LABEL, clientId, DEFAULT_HDR_COL1_LABEL);
        String hdr1Val   = resolveColumnValue(headerPO, hdr1Cfg);
        if (isEmpty(hdr1Val))
            hdr1Val = tableName + " #" + headerPO.get_ID();
        setLabelWithTitle(lHdrCol1, lHdrCol1Title, hdr1Val, hdr1Label);

        // HDR_COL2 — Business Partner / Grup / pihak lain
        String hdr2Cfg   = getSysConfig(tableName, HDR_COL2, clientId, DEFAULT_HDR_COL2);
        String hdr2Label = getSysConfig(tableName, HDR_COL2_LABEL, clientId, DEFAULT_HDR_COL2_LABEL);
        String hdr2Val   = resolveColumnValue(headerPO, hdr2Cfg);
        if (isEmpty(hdr2Val))
            hdr2Val = resolveCreatedByName(headerPO);
        setLabelWithTitle(lHdrCol2, lHdrCol2Title, hdr2Val, hdr2Label);

        // HDR_COL3 — Tanggal
        String hdr3Cfg   = getSysConfig(tableName, HDR_COL3, clientId, DEFAULT_HDR_COL3);
        String hdr3Label = getSysConfig(tableName, HDR_COL3_LABEL, clientId, DEFAULT_HDR_COL3_LABEL);
        Object rawDate   = resolveColumnObject(headerPO, hdr3Cfg);
        String hdr3Val   = rawDate != null ? formatDate(rawDate) : "-";
        setLabelWithTitle(lHdrCol3, lHdrCol3Title, hdr3Val, hdr3Label);

        // HDR_COL4 — Grand Total / info lain (type-aware)
        String hdr4Cfg   = getSysConfig(tableName, HDR_COL4, clientId, DEFAULT_HDR_COL4);
        String hdr4Label = getSysConfig(tableName, HDR_COL4_LABEL, clientId, DEFAULT_HDR_COL4_LABEL);
        String hdr4Type  = getSysConfig(tableName, HDR_COL4_TYPE,  clientId, DEFAULT_HDR_COL4_TYPE);
        String hdr4Raw   = resolveColumnValue(headerPO, hdr4Cfg);
        String hdr4Val   = formatByType(hdr4Raw, hdr4Type, "HDR_COL4[" + tableName + "]");
        setLabelWithTitle(lHdrCol4, lHdrCol4Title, hdr4Val, hdr4Label);

        // Caption groupbox
        setGroupboxCaption("Detail: " + tableName + "  #" + headerPO.get_ID());
    }

    private void renderLines(PO headerPO, String tableName, int recordId, int clientId) {

        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        String linkCol   = getSysConfig(tableName, LINK_COL,   clientId, null);

        if (isEmpty(lineTable) || "-".equals(lineTable)) {
            lstTxLines.setVisible(false);
            return;
        }

        if (isEmpty(linkCol) || "-".equals(linkCol)) {
            lstTxLines.setVisible(true);
            renderListhead(tableName, clientId); // tetap render header meski error
            appendInfoRow("Konfigurasi tidak lengkap: LINK_COL untuk "
                    + tableName + " belum diisi.");
            return;
        }

        // Baca konfigurasi kolom beserta label dan type
        String col1Cfg   = getSysConfig(tableName, COL1,      clientId, DEFAULT_COL1);
        String col1Type  = getSysConfig(tableName, COL1_TYPE, clientId, DEFAULT_COL1_TYPE);

        String col2Cfg   = getSysConfig(tableName, COL2,      clientId, DEFAULT_COL2);
        String col2Type  = getSysConfig(tableName, COL2_TYPE, clientId, DEFAULT_COL2_TYPE);

        String col3Cfg   = getSysConfig(tableName, COL3,      clientId, DEFAULT_COL3);
        String col3Type  = getSysConfig(tableName, COL3_TYPE, clientId, DEFAULT_COL3_TYPE);

        String orderByCfg = getSysConfig(tableName, ORDER_BY, clientId, linkCol);

        // Render Listhead dinamis sesuai konfigurasi label
        renderListhead(tableName, clientId);

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

                // COL1 — selalu string (deskripsi/nama produk/alamat)
                String val1 = resolveColumnValue(line, col1Cfg);
                if (isEmpty(val1)) val1 = "Item #" + line.get_ID();

                // COL2 — type-aware
                String raw2 = resolveColumnValue(line, col2Cfg);
                String val2 = formatByType(raw2, col2Type,
                        "COL2[" + tableName + "] line#" + line.get_ID());

                // COL3 — type-aware
                String raw3 = resolveColumnValue(line, col3Cfg);
                String val3 = formatByType(raw3, col3Type,
                        "COL3[" + tableName + "] line#" + line.get_ID());

                Listitem item = new Listitem();
                item.appendChild(new Listcell(val1));
                item.appendChild(new Listcell(val2));
                item.appendChild(new Listcell(val3));
                lstTxLines.appendChild(item);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "renderLines gagal: lineTable=" + lineTable
                    + " linkCol=" + linkCol + " orderBy=" + orderByCfg, e);
            appendInfoRow("Error memuat baris: " + e.getMessage());
        }
    }

    /**
     * Rebuild Listhead di lstTxLines berdasarkan konfigurasi _COL1_LABEL,
     * _COL2_LABEL, _COL3_LABEL.
     *
     * Listhead lama dihapus terlebih dahulu setiap kali render dipanggil
     * agar tidak terjadi duplikasi header.
     *
     * Kolom yang dinonaktifkan dengan "-" tetap ditampilkan sebagai header
     * kosong agar layout tabel tidak bergeser.
     */
    private void renderListhead(String tableName, int clientId) {
        // Hapus Listhead lama jika ada
        if (lstTxLines.getListhead() != null)
            lstTxLines.removeChild(lstTxLines.getListhead());

        String label1 = getSysConfig(tableName, COL1_LABEL, clientId, DEFAULT_COL1_LABEL);
        String label2 = getSysConfig(tableName, COL2_LABEL, clientId, DEFAULT_COL2_LABEL);
        String label3 = getSysConfig(tableName, COL3_LABEL, clientId, DEFAULT_COL3_LABEL);

        // "-" sebagai label → tampilkan kosong
        if ("-".equals(label1)) label1 = "";
        if ("-".equals(label2)) label2 = "";
        if ("-".equals(label3)) label3 = "";

        Listhead head = new Listhead();
        head.setSizable(true);
        head.appendChild(new Listheader(label1));
        head.appendChild(new Listheader(label2));
        head.appendChild(new Listheader(label3));

        lstTxLines.insertBefore(head, lstTxLines.getFirstChild());
    }
    private void validateAndWarnConfig(String tableName, int clientId) {
        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        if (lineTable == null) return;

        String linkCol = getSysConfig(tableName, LINK_COL, clientId, null);
        if (isEmpty(linkCol) || "-".equals(linkCol)) {
            log.warning("[WFDetail] " + tableName
                    + ": LINE_TABLE=" + lineTable
                    + " dikonfigurasi tapi LINK_COL kosong atau '-'."
                    + " Lines tidak akan ditampilkan.");
        }

        if ("-".equals(lineTable)) return;

        String col2Cfg = getSysConfig(tableName, COL2, clientId, null);
        if (col2Cfg == null)
            log.info("[WFDetail] " + tableName
                    + ": COL2 tidak dikonfigurasi, pakai default: " + DEFAULT_COL2);

        String col3Cfg = getSysConfig(tableName, COL3, clientId, null);
        if (col3Cfg == null)
            log.info("[WFDetail] " + tableName
                    + ": COL3 tidak dikonfigurasi, pakai default: " + DEFAULT_COL3);

        String orderBy = getSysConfig(tableName, ORDER_BY, clientId, null);
        if (orderBy == null)
            log.info("[WFDetail] " + tableName
                    + ": ORDER_BY tidak dikonfigurasi, fallback ke LINK_COL=" + linkCol);
    }

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

    /**
     * Resolve nilai String dari daftar kolom (comma-separated).
     * Dicoba satu per satu, return nilai pertama yang tidak kosong/whitespace.
     *
     * Mendukung notasi FK multi-level: "C_BPartner_ID>C_BPartner_Location_ID>C_Location_ID>City"
     * Untuk skip: "-"
     */
    private String resolveColumnValue(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : columnDefs.split(",")) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;

            String value = resolveOneColumn(po, candidate);
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
     * Resolve satu definisi kolom. Mendukung multi-level FK chain.
     *
     * Format biasa      : "DocumentNo"
     * Format FK 1 level : "C_BPartner_ID>Name"
     * Format FK N level : "C_BPartner_ID>C_BPartner_Location_ID>C_Location_ID>City"
     *
     * Untuk FK, nama tabel di-derive dari nama kolom:
     *   C_BPartner_ID → tabel C_BPartner
     *   M_Product_ID  → tabel M_Product
     */
    private String resolveOneColumn(PO po, String columnDef) {
        if (columnDef == null || columnDef.isEmpty()) return null;

        if (!columnDef.contains(">")) {
            // Kolom biasa
            Object val = po.get_Value(columnDef);
            return val != null ? val.toString().trim() : null;
        }

        // Multi-level FK: split semua segment
        // segment[0..n-2] = FK columns, segment[n-1] = target column
        String[] segments  = columnDef.split(">");
        String   targetCol = segments[segments.length - 1].trim();
        PO       currentPO = po;

        for (int i = 0; i < segments.length - 1; i++) {
            String fkCol = segments[i].trim();

            int fkId = currentPO.get_ValueAsInt(fkCol);
            if (fkId <= 0) {
                log.fine("[WFDetail] FK bernilai 0/null pada segment '"
                        + fkCol + "' di chain '" + columnDef + "'");
                return null;
            }

            // Derive nama tabel: C_BPartner_ID → C_BPartner
            String fkTableName = fkCol.endsWith("_ID")
                    ? fkCol.substring(0, fkCol.length() - 3)
                    : fkCol;

            try {
                MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
                if (fkTable == null) {
                    log.warning("[WFDetail] FK table tidak ditemukan: '"
                            + fkTableName + "' (segment '" + fkCol
                            + "' di chain '" + columnDef + "')");
                    return null;
                }

                PO fkPO = fkTable.getPO(fkId, null);
                if (fkPO == null) {
                    log.warning("[WFDetail] Record tidak ditemukan: "
                            + fkTableName + " #" + fkId);
                    return null;
                }

                currentPO = fkPO; // lanjut ke level berikutnya

            } catch (Exception e) {
                log.warning("[WFDetail] FK lookup gagal [segment=" + fkCol
                        + ", chain=" + columnDef + "]: " + e.getMessage());
                return null;
            }
        }

        Object val = currentPO.get_Value(targetCol);
        return val != null ? val.toString().trim() : null;
    }

    /**
     * Format nilai berdasarkan tipe yang dikonfigurasi.
     *
     * numeric : validasi angka → format ribuan 2 desimal. Jika bukan angka → "0"
     * string  : tampil apa adanya. Null/kosong → "-"
     *
     * @param raw     nilai mentah dari PO
     * @param type    "numeric" atau "string"
     * @param context label untuk logging
     */
    private String formatByType(String raw, String type, String context) {
        if (isEmpty(raw) || "-".equals(raw))
            return TYPE_STRING.equals(type) ? "-" : "0";

        if (TYPE_STRING.equals(type))
            return raw; // string — tampil apa adanya

        // numeric path
        if (!isNumericString(raw)) {
            log.warning("[WFDetail] " + context
                    + ": nilai '" + raw + "' bukan numerik."
                    + " Periksa konfigurasi _TYPE atau nama kolom di SysConfig."
                    + " Ditampilkan sebagai '0'.");
            return "0";
        }

        return formatAmount(raw, context);
    }

    /**
     * Cek apakah string adalah angka valid (termasuk desimal dan negatif).
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

    /**
     * Set nilai label dan title (judul field) secara bersamaan.
     * lTitle boleh null — jika null, diabaikan.
     */
    private void setLabelWithTitle(Label lValue, Label lTitle,
                                   String value, String title) {
        if (lValue != null)
            lValue.setValue(isEmpty(value) ? "-" : value);
        if (lTitle != null)
            lTitle.setValue(isEmpty(title) || "-".equals(title) ? "" : title);
    }

    /**
     * Reset semua UI component ke kondisi awal / kosong.
     * Hapus hanya Listitem — Listhead akan di-rebuild oleh renderListhead().
     */
    private void clearUI() {
        List<Listitem> toRemove = new ArrayList<>(lstTxLines.getItems());
        for (Listitem item : toRemove)
            lstTxLines.removeChild(item);

        // Reset nilai
        setLabelWithTitle(lHdrCol1, lHdrCol1Title, "-", "");
        setLabelWithTitle(lHdrCol2, lHdrCol2Title, "-", "");
        setLabelWithTitle(lHdrCol3, lHdrCol3Title, "-", "");
        setLabelWithTitle(lHdrCol4, lHdrCol4Title, "-", "");

        setGroupboxCaption("Detail Transaksi");
        grpTxDetails.setVisible(false);
    }
    
    private void setGroupboxCaption(String text) {
    // Hapus Caption lama jika ada
    org.zkoss.zul.Caption existing = grpTxDetails.getCaption();
    if (existing != null)
        grpTxDetails.removeChild(existing);

    org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption(text);
    grpTxDetails.appendChild(cp);
    }
    /**
     * Tambahkan satu baris informasi/pesan di Listbox lines.
     */
    private void appendInfoRow(String message) {
        Listitem item = new Listitem();
        item.appendChild(new Listcell(message));
        item.appendChild(new Listcell("-"));
        item.appendChild(new Listcell("-"));
        lstTxLines.appendChild(item);
    }

    /**
     * Null-safe empty check.
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
