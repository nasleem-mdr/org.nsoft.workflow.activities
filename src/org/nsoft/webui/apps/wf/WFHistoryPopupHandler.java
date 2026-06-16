/*******************************************************************************
 * WFHistoryPopupHandler.java
 *
 * Handler class for displaying price history & related document popup
 * when an approver clicks a line item in the WF Approval form.
 *
 * Responsibilities:
 *  - Build and open ZK Popup anchored to the clicked Listitem
 *  - Query product price history + related documents (single SQL, no N+1)
 *  - Manage popup lifecycle (open / close / replace)
 *
 * Usage:
 *  1. Instantiate once in WFTransactionDetailRenderer as a field
 *  2. Call open() from renderLines() onClick listener
 *  3. Call close() from clearUI()
 *
 * Future extension:
 *  - WFAccountingPopupHandler follows the same pattern for accounting preview
 *
 * Versi : 1.0
 ******************************************************************************/
package org.nsoft.webui.apps.wf;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vlayout;


public class WFHistoryPopupHandler {

    private static final CLogger log = CLogger.getCLogger(WFHistoryPopupHandler.class);

    // SysConfig key constants — shared pattern with WFTransactionDetailRenderer
    private static final String PREFIX     = "WF_DETAIL_";
    private static final String LINE_TABLE = "_LINE_TABLE";
    private static final String LINK_COL   = "_LINK_COL";
    private static final String COL2       = "_COL2";
    private static final String COL3       = "_COL3";
    private static final String HDR_COL1   = "_HDR_COL1";
    private static final String HDR_COL3   = "_HDR_COL3";

    // Default fallbacks — mirror dari WFTransactionDetailRenderer
    private static final String DEFAULT_COL2     = "QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty";
    private static final String DEFAULT_COL3     = "LineNetAmt,PriceActual,-";
    private static final String DEFAULT_HDR_COL1 = "DocumentNo,Value,Name";
    private static final String DEFAULT_HDR_COL3 = "DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created";

    private static final int HISTORY_LIMIT = 3;

    // Active popup — hanya satu yang bisa terbuka sekaligus
    private Popup activePopup = null;

       // =========================================================================
    // PUBLIC API
    // =========================================================================
    /**
     * Buka popup riwayat untuk line item yang diklik.
     * Jika ada popup lain yang sedang terbuka, akan ditutup dulu.
     * 
     * @param linePO      PO dari baris line yang diklik
     * @param itemLabel   label teks item untuk judul popup
     * @param anchor      ZK component sebagai anchor posisi popup (biasanya Listitem)
     * @param tableName   nama header table (dari SysConfig context)
     * @param clientId    AD_Client_ID
     * @param headerPO    PO header aktif (untuk exclude dari hasil history)
     */
    public void open(PO linePO, String itemLabel, org.zkoss.zk.ui.Component anchor, String tableName, int clientId, PO headerPO) {
        close();
        
        // Pastikan anchor dan page tidak null untuk mencegah NullPointerException (NPE)
        if (anchor == null || anchor.getPage() == null) {
            log.warning("[WFHistory] Pembukaan popup dibatalkan: Komponen anchor tidak terpasang (detached) ke Page.");
            return;
        }
        
        int productId = linePO.get_ValueAsInt("M_Product_ID");
        Popup popup = buildPopup(linePO, itemLabel, productId, tableName, clientId, headerPO);
        
        // Aman untuk dijalankan karena page sudah divalidasi tidak null
        anchor.getPage().addComponent(popup);
        activePopup = popup;
        popup.open(anchor, "after_start");
    }
    /**
     * Tutup dan detach popup yang sedang aktif.
     * Aman dipanggil meskipun tidak ada popup yang terbuka.
     */
    public void close() {
        if (activePopup != null) {
            try {
                if (activePopup.getPage() != null)
                    activePopup.getPage().removeComponent(activePopup);
                activePopup.detach();
            } catch (Exception ignored) {}
            activePopup = null;
        }
    }

    // =========================================================================
    // PRIVATE — POPUP BUILD
    // =========================================================================

    private Popup buildPopup(PO linePO, String itemLabel, int productId,
                             String tableName, int clientId, PO headerPO) {

        Popup popup = new Popup();
        popup.setStyle(
            "background:var(--color-surface,#fff);" +
            "border:1px solid var(--color-border,#d1d5db);" +
            "border-radius:8px;" +
            "box-shadow:0 4px 16px rgba(0,0,0,0.12);" +
            "padding:0;" +
            "min-width:400px;" +
            "max-width:540px;"
        );

        Vlayout layout = new Vlayout();
        layout.setStyle("padding:12px 14px;gap:0;");

        // --- Judul ---
        Label title = new Label("📋 Riwayat: " + itemLabel);
        title.setStyle(
            "font-weight:700;" +
            "font-size:13px;" +
            "color:var(--color-text,#111827);" +
            "display:block;" +
            "margin-bottom:8px;" +
            "padding-bottom:6px;" +
            "border-bottom:1px solid var(--color-border,#e5e7eb);"
        );
        layout.appendChild(title);

        // --- Konten ---
        if (productId > 0) {
            List<HistoryRow> rows = queryHistory(productId, tableName, clientId, headerPO);
            appendHistoryGrid(layout, rows);
        } else {
            Label noProduct = new Label("ℹ️ Item ini tidak memiliki M_Product_ID.");
            noProduct.setStyle("color:#6b7280;font-size:12px;");
            layout.appendChild(noProduct);
        }

        // --- Tombol tutup ---
        Separator sep = new Separator();
        sep.setStyle("margin:8px 0 4px;");
        layout.appendChild(sep);

        Button btnClose = new Button("Tutup");
        btnClose.setStyle("font-size:11px;padding:2px 10px;border-radius:4px;cursor:pointer;");
        btnClose.addEventListener(Events.ON_CLICK, e -> close());
        layout.appendChild(btnClose);

        popup.appendChild(layout);
        return popup;
    }
    /**
     * Validation: is string value from SysConfig filled by secure character.
     * Valid character (a-z, A-Z), numbe (0-9), and underscore (_).
    */
    private boolean isValidSQLIdentifier(String identifier) {
        if (isEmpty(identifier)) 
            return false;
        // Regex: ^[a-zA-Z0-9_]+$
        return identifier.matches("^[a-zA-Z0-9_]+$");
    }
    // =========================================================================
    // PRIVATE — QUERY
    // =========================================================================

    /**
     * Query riwayat harga & dokumen untuk produk yang sama.
     *
     * Single SQL dengan JOIN ke header table dan C_BPartner —
     * tidak ada N+1 query.
     *
     * Exclude dokumen yang sedang diapprove (headerPO) dari hasil.
     */
        private List<HistoryRow> queryHistory(int productId,
                                          String tableName,
                                          int clientId,
                                          PO headerPO) {
        List<HistoryRow> result = new ArrayList<>();
    
        // 1. Validasi awal untuk parameter tableName dari context
        if (!isValidSQLIdentifier(tableName)) {
            log.warning("[WFHistory] Security Alert: Nama tabel utama tidak valid: " + tableName);
            return result;
        }
    
        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        String linkCol   = getSysConfig(tableName, LINK_COL,   clientId, null);
    
        if (isEmpty(lineTable) || "-".equals(lineTable)
                || isEmpty(linkCol) || "-".equals(linkCol)) {
            log.info("[WFHistory] Skipping — LINE_TABLE/LINK_COL not configured for: " + tableName);
            return result;
        }
    
        // 2. Validasi ketat untuk lineTable dan linkCol dari SysConfig
        if (!isValidSQLIdentifier(lineTable) || !isValidSQLIdentifier(linkCol)) {
            log.warning("[WFHistory] Security Alert: LINE_TABLE atau LINK_COL mengandung karakter ilegal!");
            return result;
        }
    
        String qtyCol      = resolveFirstSimpleColumn(getSysConfig(tableName, COL2, clientId, DEFAULT_COL2));
        String priceCol    = resolveFirstSimpleColumn(getSysConfig(tableName, COL3, clientId, DEFAULT_COL3));
        String hdrDocNoCol = resolveFirstSimpleColumn(getSysConfig(tableName, HDR_COL1, clientId, DEFAULT_HDR_COL1));
        String hdrDateCol  = resolveFirstSimpleColumn(getSysConfig(tableName, HDR_COL3, clientId, DEFAULT_HDR_COL3));
    
        // 3. Validasi ketat untuk kolom-kolom dinamis sebelum ditempel ke SQL
        if (!isEmpty(qtyCol) && !isValidSQLIdentifier(qtyCol)) {
            log.warning("[WFHistory] Security Alert: qtyCol tidak valid: " + qtyCol);
            return result;
        }
        if (!isEmpty(priceCol) && !isValidSQLIdentifier(priceCol)) {
            log.warning("[WFHistory] Security Alert: priceCol tidak valid: " + priceCol);
            return result;
        }
        if (!isEmpty(hdrDocNoCol) && !isValidSQLIdentifier(hdrDocNoCol)) {
            log.warning("[WFHistory] Security Alert: hdrDocNoCol tidak valid: " + hdrDocNoCol);
            return result;
        }
        if (!isEmpty(hdrDateCol) && !isValidSQLIdentifier(hdrDateCol)) {
            log.warning("[WFHistory] Security Alert: hdrDateCol tidak valid: " + hdrDateCol);
            return result;
        }
    
        int currentHeaderId = headerPO != null ? headerPO.get_ID() : 0;
    
        // --- SIFAT SQL SEKARANG AMAN KARENA SEMUA IDENTIFIER SUDAH DI-WHITELIST ---
        StringBuilder sql = new StringBuilder("SELECT ");
        if (!isEmpty(qtyCol))      sql.append("l.").append(qtyCol).append(", ");
        if (!isEmpty(priceCol))    sql.append("l.").append(priceCol).append(", ");
        if (!isEmpty(hdrDocNoCol)) sql.append("h.").append(hdrDocNoCol).append(", ");
        if (!isEmpty(hdrDateCol))  sql.append("h.").append(hdrDateCol).append(", ");
        sql.append("bp.Name AS BPartnerName ");
    
        sql.append("FROM ").append(lineTable).append(" l ");
        sql.append("LEFT JOIN ").append(tableName).append(" h ");
        sql.append(  "ON h.").append(tableName).append("_ID = l.").append(linkCol).append(" ");
        sql.append("LEFT JOIN C_BPartner bp ON bp.C_BPartner_ID = h.C_BPartner_ID ");
    
        sql.append("WHERE l.M_Product_ID = ? ");
        if (currentHeaderId > 0)
            sql.append("AND l.").append(linkCol).append(" != ? ");
        sql.append("AND l.AD_Client_ID = ? ");
        sql.append("AND l.IsActive = 'Y' ");
    
        sql.append("ORDER BY h.")
           .append(!isEmpty(hdrDateCol) ? hdrDateCol : "Created")
           .append(" DESC ");
        sql.append("LIMIT ").append(HISTORY_LIMIT);

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql.toString(), null);
            int idx = 1;
            pstmt.setInt(idx++, productId);
            if (currentHeaderId > 0)
                pstmt.setInt(idx++, currentHeaderId);
            pstmt.setInt(idx, clientId);

            rs = pstmt.executeQuery();

            while (rs.next()) {
                HistoryRow row = new HistoryRow();

                if (!isEmpty(qtyCol)) {
                    BigDecimal qtyValue = rs.getBigDecimal(qtyCol);
                    row.qty = qtyValue != null ? formatBigDecimal(qtyValue) : "-";
                }

                if (!isEmpty(priceCol)) {
                    BigDecimal priceValue = rs.getBigDecimal(priceCol);
                    row.price = priceValue != null ? formatBigDecimal(priceValue) : "-";
                }

                if (!isEmpty(hdrDocNoCol)) {
                    Object val = rs.getObject(hdrDocNoCol);
                    row.documentNo = val != null ? val.toString() : "-";
                }

                if (!isEmpty(hdrDateCol)) {
                    Object val = rs.getObject(hdrDateCol);
                    row.date = val != null ? formatDate(val) : "-";
                }

                String bpName = rs.getString("BPartnerName");
                row.bpartner = bpName != null ? bpName : "-";

                result.add(row);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "[WFHistory] Query gagal untuk productId="
                    + productId + " table=" + tableName, e);
        } finally {
            DB.close(rs, pstmt);
        }

        return result;
    }

    // =========================================================================
    // PRIVATE — RENDER GRID
    // =========================================================================

    private void appendHistoryGrid(Vlayout layout, List<HistoryRow> rows) {

        if (rows == null || rows.isEmpty()) {
            Label empty = new Label("Belum ada riwayat transaksi untuk item ini.");
            empty.setStyle("color:#6b7280;font-size:12px;font-style:italic;");
            layout.appendChild(empty);
            return;
        }

        Label subTitle = new Label("📊 " + rows.size() + " transaksi terakhir (maks. " + HISTORY_LIMIT + "):");
        subTitle.setStyle("font-size:11px;color:#6b7280;margin-bottom:6px;display:block;");
        layout.appendChild(subTitle);

        Grid grid = new Grid();
        grid.setStyle("width:100%;font-size:12px;");
        grid.setSclass("history-popup-grid");

        // Header kolom
        Columns cols = new Columns();
        String[] headers = {"No. Dokumen", "Tanggal", "Business Partner", "Qty", "Harga"};
        String[] widths  = {"22%", "16%", "28%", "14%", "20%"};
        for (int i = 0; i < headers.length; i++) {
            Column col = new Column(headers[i]);
            col.setWidth(widths[i]);
            col.setStyle("font-size:11px;color:#374151;background:#f3f4f6;padding:4px 6px;");
            cols.appendChild(col);
        }
        grid.appendChild(cols);

        // Data rows
        Rows rowsComp = new Rows();
        for (HistoryRow hr : rows) {
            Row row = new Row();
            row.setStyle("border-bottom:1px solid #f3f4f6;");

            appendCell(row, hr.documentNo, "color:#1d4ed8;font-weight:600;");
            appendCell(row, hr.date,       "");
            appendCell(row, hr.bpartner,   "");
            appendCell(row, hr.qty,        "text-align:right;");
            appendCell(row, hr.price,      "text-align:right;font-weight:600;color:#065f46;");

            rowsComp.appendChild(row);
        }
        grid.appendChild(rowsComp);
        layout.appendChild(grid);
    }

    private void appendCell(Row row, String value, String style) {
        Cell cell = new Cell();
        cell.setStyle("padding:4px 6px;" + (isEmpty(style) ? "" : style));
        cell.appendChild(new Label(isEmpty(value) ? "-" : value));
        row.appendChild(cell);
    }

    // =========================================================================
    // PRIVATE — HELPERS
    // =========================================================================

    private String getSysConfig(String tableName, String suffix,
                                int clientId, String defaultValue) {
        String key   = PREFIX + tableName + suffix;
        String value = MSysConfig.getValue(key, null, clientId);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    /**
     * Ambil kandidat kolom pertama yang bukan FK-chain (tidak mengandung '>').
     * Digunakan untuk memetakan SysConfig column-def ke nama kolom SQL aktual.
     */
    private String resolveFirstSimpleColumn(String columnDefs) {
        if (isEmpty(columnDefs)) return null;
        for (String candidate : splitColumnDefs(columnDefs)) {
            candidate = candidate.trim();
            if (!candidate.isEmpty() && !"-".equals(candidate) && !candidate.contains(">"))
                return candidate;
        }
        return null;
    }

    /**
     * Split column defs dengan aturan:
     * koma adalah pemisah kandidat, KECUALI koma yang berada di dalam FK-chain (setelah '>').
     *
     * Contoh: "M_Product_ID>Name,Description,Name"
     *   → ["M_Product_ID>Name", "Description", "Name"]
     *
     * Mirror dari splitColumnDefs() di WFTransactionDetailRenderer.
     */
    private List<String> splitColumnDefs(String columnDefs) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideFKChain = false;

        for (char c : columnDefs.toCharArray()) {
            if (c == '>') {
                insideFKChain = true;
                current.append(c);
            } else if (c == ',') {
                insideFKChain = false;
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0)
            result.add(current.toString());

        return result;
    }

    private String formatAmount(String rawValue) {
        if (isEmpty(rawValue) || "-".equals(rawValue)) return "-";
        try {
            BigDecimal bd = new BigDecimal(rawValue);
            NumberFormat nf = NumberFormat.getNumberInstance(
                    Env.getLanguage(Env.getCtx()).getLocale());
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(bd);
        } catch (NumberFormatException e) {
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

            return new SimpleDateFormat("dd MMM yyyy",
                    Env.getLanguage(Env.getCtx()).getLocale()).format(date);
        } catch (Exception e) {
            return dateObj.toString();
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // =========================================================================
    // INNER CLASS — Data holder
    // =========================================================================

    private static class HistoryRow {
        String documentNo = "-";
        String date       = "-";
        String bpartner   = "-";
        String qty        = "-";
        String price      = "-";
    }
}
