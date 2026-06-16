/*******************************************************************************
 * WFTransactionDetailRenderer.java
 *
 * Renderer class to query document details
 * at Workflow Approval (WWFActivity) Form
 *
 * Architecture : SysConfig-driven + Generic PO + SQL Query
 * Versi        : 4.0 — Auto-label dari MTable.getName() (multi-bahasa),
 *                getLabel() priority chain, validation improvements,
 *                appendInfoRow colspan fix
 ******************************************************************************/
package org.nsoft.webui.apps.wf;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MColumn;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Column;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Separator;
import org.compiere.util.DB;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class WFTransactionDetailRenderer {

    private static final CLogger log = CLogger.getCLogger(WFTransactionDetailRenderer.class);

    // SYSCONFIG KEY CONSTANTS
    private static final String PREFIX         = "WF_DETAIL_";

    // --- Line table ---
    private static final String LINE_TABLE     = "_LINE_TABLE";
    private static final String LINK_COL       = "_LINK_COL";
    private static final String ORDER_BY       = "_ORDER_BY";

    // --- Line columns ---
    private static final String COL1           = "_COL1";
    private static final String COL1_LABEL     = "_COL1_LABEL";
    private static final String COL1_TYPE      = "_COL1_TYPE";

    private static final String COL2           = "_COL2";
    private static final String COL2_LABEL     = "_COL2_LABEL";
    private static final String COL2_TYPE      = "_COL2_TYPE";

    private static final String COL3           = "_COL3";
    private static final String COL3_LABEL     = "_COL3_LABEL";
    private static final String COL3_TYPE      = "_COL3_TYPE";

    // --- Header fields ---
    private static final String HDR_COL1       = "_HDR_COL1";
    private static final String HDR_COL1_LABEL = "_HDR_COL1_LABEL";

    private static final String HDR_COL2       = "_HDR_COL2";
    private static final String HDR_COL2_LABEL = "_HDR_COL2_LABEL";

    private static final String HDR_COL3       = "_HDR_COL3";
    private static final String HDR_COL3_LABEL = "_HDR_COL3_LABEL";

    private static final String HDR_COL4       = "_HDR_COL4";
    private static final String HDR_COL4_LABEL = "_HDR_COL4_LABEL";
    private static final String HDR_COL4_TYPE  = "_HDR_COL4_TYPE";

    // TYPE CONSTANTS
    private static final String TYPE_NUMERIC   = "numeric";
    private static final String TYPE_STRING    = "string";

    // DEFAULT FALLBACK VALUES
    private static final String DEFAULT_COL1       = "M_Product_ID>Name,Description,Name";
    private static final String DEFAULT_COL1_LABEL = "Description";
    private static final String DEFAULT_COL1_TYPE  = TYPE_STRING;

    private static final String DEFAULT_COL2       = "QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty";
    private static final String DEFAULT_COL2_LABEL = "Qty";
    private static final String DEFAULT_COL2_TYPE  = TYPE_NUMERIC;

    private static final String DEFAULT_COL3       = "LineNetAmt,PriceActual,-";
    private static final String DEFAULT_COL3_LABEL = "Amount";
    private static final String DEFAULT_COL3_TYPE  = TYPE_NUMERIC;

    private static final String DEFAULT_HDR_COL1       = "DocumentNo,Value,Name";
    private static final String DEFAULT_HDR_COL1_LABEL = "Document No";

    private static final String DEFAULT_HDR_COL2       = "C_BPartner_ID>Name";
    private static final String DEFAULT_HDR_COL2_LABEL = "Business Partner";

    private static final String DEFAULT_HDR_COL3       = "DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created";
    private static final String DEFAULT_HDR_COL3_LABEL = "Date";

    private static final String DEFAULT_HDR_COL4       = "GrandTotal,TotalLines,-";
    private static final String DEFAULT_HDR_COL4_LABEL = "Total";
    private static final String DEFAULT_HDR_COL4_TYPE  = TYPE_NUMERIC;

    // UI COMPONENTS
    private final Groupbox grpTxDetails;
    private final Listbox  lstTxLines;
    private final Label    lHdrCol1;
    private final Label    lHdrCol2;
    private final Label    lHdrCol3;
    private final Label    lHdrCol4;

    private final Label    lHdrCol1Title;
    private final Label    lHdrCol2Title;
    private final Label    lHdrCol3Title;
    private final Label    lHdrCol4Title;
      
    // WFHistoryPopupHandler
    private final WFHistoryPopupHandler historyHandler = new WFHistoryPopupHandler();
    // CONSTRUCTOR — minimum
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

    // CONSTRUCTOR — All
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
     * Main entry point. Called from WWFActivity.display() every time
     * an approval item is selected in the left listbox.
     *
     * @param activity Active MWFActivity, can be null
     */
   public void render(MWFActivity activity) {
        clearUI();
        closeActivePopup(); // tutup popup lama kalau ada
    
        if (activity == null || activity.getRecord_ID() <= 0) {
            grpTxDetails.setVisible(false);
            return;
        }
    
        String tableName = "(unknown)";
        int    recordId  = 0;
    
        try {
            int    clientId = activity.getAD_Client_ID();
            int    tableId  = activity.getAD_Table_ID();
            recordId        = activity.getRecord_ID();
            MTable table    = MTable.get(Env.getCtx(), tableId);
            tableName       = table.getTableName();
    
            // ★ simpan konteks untuk dipakai popup
            this.currentTableName = tableName;
            this.currentClientId  = clientId;
    
            validateAndWarnConfig(tableName, clientId);
    
            PO headerPO = table.getPO(recordId, null);
            if (headerPO == null) {
                setGroupboxCaption("Detail (" + tableName + " #" + recordId + " Not Found)");
                grpTxDetails.setVisible(true);
                return;
            }
    
            // ★ simpan header PO untuk konteks popup
            this.currentHeaderPO = headerPO;
    
            renderHeader(headerPO, tableName, clientId);
            renderLines(headerPO, tableName, recordId, clientId);
    
            grpTxDetails.setVisible(true);
    
        } catch (Exception e) {
            log.log(Level.SEVERE, "WFTransactionDetailRenderer.render() gagal", e);
            setGroupboxCaption("Detail (" + tableName + " #" + recordId
                    + " Error: " + e.getMessage() + ")");
            grpTxDetails.setVisible(true);
        }
    }
    // =========================================================================
    // PRIVATE — RENDER
    // =========================================================================

    private void renderHeader(PO headerPO, String tableName, int clientId) {

        // HDR_COL1 — Document No / Name / Value
        String hdr1Cfg   = getSysConfig(tableName, HDR_COL1, clientId, DEFAULT_HDR_COL1);
        String hdr1Label = getLabel(tableName, HDR_COL1_LABEL, clientId, hdr1Cfg, DEFAULT_HDR_COL1_LABEL);
        String hdr1Val   = resolveColumnValue(headerPO, hdr1Cfg);
        if (isEmpty(hdr1Val))
            hdr1Val = tableName + " #" + headerPO.get_ID();
        setLabelWithTitle(lHdrCol1, lHdrCol1Title, hdr1Val, hdr1Label);

        // HDR_COL2 — Business Partner / Group
        String hdr2Cfg   = getSysConfig(tableName, HDR_COL2, clientId, DEFAULT_HDR_COL2);
        String hdr2Label = getLabel(tableName, HDR_COL2_LABEL, clientId, hdr2Cfg, DEFAULT_HDR_COL2_LABEL);
        String hdr2Val   = resolveColumnValue(headerPO, hdr2Cfg);
        if (isEmpty(hdr2Val))
            hdr2Val = resolveCreatedByName(headerPO);
        setLabelWithTitle(lHdrCol2, lHdrCol2Title, hdr2Val, hdr2Label);

        // HDR_COL3 — Date
        String hdr3Cfg   = getSysConfig(tableName, HDR_COL3, clientId, DEFAULT_HDR_COL3);
        String hdr3Label = getLabel(tableName, HDR_COL3_LABEL, clientId, hdr3Cfg, DEFAULT_HDR_COL3_LABEL);
        Object rawDate   = resolveColumnObject(headerPO, hdr3Cfg);
        String hdr3Val   = rawDate != null ? formatDate(rawDate) : "-";
        setLabelWithTitle(lHdrCol3, lHdrCol3Title, hdr3Val, hdr3Label);

        // HDR_COL4 — Grand Total / other info (type-aware)
        String hdr4Cfg   = getSysConfig(tableName, HDR_COL4, clientId, DEFAULT_HDR_COL4);
        String hdr4Label = getLabel(tableName, HDR_COL4_LABEL, clientId, hdr4Cfg, DEFAULT_HDR_COL4_LABEL);
        String hdr4Type  = getSysConfig(tableName, HDR_COL4_TYPE, clientId, DEFAULT_HDR_COL4_TYPE);
        String hdr4Raw   = resolveColumnValue(headerPO, hdr4Cfg);
        String hdr4Val   = formatByType(hdr4Raw, hdr4Type, "HDR_COL4[" + tableName + "]");
        setLabelWithTitle(lHdrCol4, lHdrCol4Title, hdr4Val, hdr4Label);

        // Caption groupbox — pakai nama table yang sudah di-translate
        String tableDisplayName = getTableDisplayName(tableName);
        setGroupboxCaption("Detail: " + tableDisplayName + " #" + hdr1Val);
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
            renderListhead(tableName, clientId);
            appendInfoRow("Incomplete configuration: LINK_COL for "
                    + tableName + " has not been filled.");
            return;
        }

        String col1Cfg   = getSysConfig(tableName, COL1,      clientId, DEFAULT_COL1);
        String col1Type  = getSysConfig(tableName, COL1_TYPE, clientId, DEFAULT_COL1_TYPE);

        String col2Cfg   = getSysConfig(tableName, COL2,      clientId, DEFAULT_COL2);
        String col2Type  = getSysConfig(tableName, COL2_TYPE, clientId, DEFAULT_COL2_TYPE);

        String col3Cfg   = getSysConfig(tableName, COL3,      clientId, DEFAULT_COL3);
        String col3Type  = getSysConfig(tableName, COL3_TYPE, clientId, DEFAULT_COL3_TYPE);

        String orderByCfg = getSysConfig(tableName, ORDER_BY, clientId, linkCol);

        renderListhead(tableName, clientId);
        lstTxLines.setVisible(true);

        try {
            List<PO> lines = new Query(Env.getCtx(), lineTable,
                    linkCol + " = ?", null)
                    .setParameters(recordId)
                    .setOrderBy(orderByCfg)
                    .list();

            if (lines == null || lines.isEmpty()) {
                appendInfoRow("(No detail lines)");
                return;
            }

            for (PO line : lines) {

    String val1 = resolveColumnValue(line, col1Cfg);
    if (isEmpty(val1)) val1 = "Item #" + line.get_ID();

    String raw2 = resolveColumnValue(line, col2Cfg);
    String val2 = formatByType(raw2, col2Type,
            "COL2[" + tableName + "] line#" + line.get_ID());

    String raw3 = resolveColumnValue(line, col3Cfg);
    String val3 = formatByType(raw3, col3Type,
            "COL3[" + tableName + "] line#" + line.get_ID());

        Listitem item = new Listitem();
        item.appendChild(new Listcell(val1));
        
        // Kolom 2
        Listcell cell2 = new Listcell(val2);
        if (TYPE_NUMERIC.equals(col2Type)) {
            cell2.setSclass("text-numeric-right");
        }
        item.appendChild(cell2);
        
        // Kolom 3
        Listcell cell3 = new Listcell(val3);
        if (TYPE_NUMERIC.equals(col3Type)) {
            cell3.setSclass("text-numeric-right");
        }
        item.appendChild(cell3);
    
    // ★ styling agar terlihat clickable
    item.setStyle("cursor:pointer;");
    item.setTooltiptext("Klik untuk melihat riwayat item ini");

    // ★ capture final reference untuk lambda
    final PO linePO    = line;
    final String label = val1;
    // renderLines() onClick:
                
    item.addEventListener(Events.ON_CLICK, event -> {
        historyHandler.open(linePO, label, item,
                        currentTableName, currentClientId, currentHeaderPO);
    });
        lstTxLines.appendChild(item);
    }
            } catch (Exception e) {
                log.log(Level.WARNING, "renderLines failed: lineTable=" + lineTable
                        + " linkCol=" + linkCol + " orderBy=" + orderByCfg, e);
                appendInfoRow("Error loading line: " + e.getMessage());
            }
    }
    
    private void renderListhead(String tableName, int clientId) {
        if (lstTxLines.getListhead() != null)
            lstTxLines.removeChild(lstTxLines.getListhead());

        String col1Cfg = getSysConfig(tableName, COL1, clientId, DEFAULT_COL1);
        String col2Cfg = getSysConfig(tableName, COL2, clientId, DEFAULT_COL2);
        String col3Cfg = getSysConfig(tableName, COL3, clientId, DEFAULT_COL3);

        String label1 = getLabel(tableName, COL1_LABEL, clientId, col1Cfg, DEFAULT_COL1_LABEL);
        String label2 = getLabel(tableName, COL2_LABEL, clientId, col2Cfg, DEFAULT_COL2_LABEL);
        String label3 = getLabel(tableName, COL3_LABEL, clientId, col3Cfg, DEFAULT_COL3_LABEL);

        if ("-".equals(label1)) label1 = "";
        if ("-".equals(label2)) label2 = "";
        if ("-".equals(label3)) label3 = "";

        Listhead head = new Listhead();
        head.setSizable(true);
        head.appendChild(new Listheader(label1));
        head.appendChild(new Listheader(label2));
        head.appendChild(new Listheader(label3));

        lstTxLines.appendChild(head);
    }

    
    private void validateAndWarnConfig(String tableName, int clientId) {
        String lineTable = getSysConfig(tableName, LINE_TABLE, clientId, null);
        if (lineTable == null) return;
        if ("-".equals(lineTable)) return;

        String linkCol = getSysConfig(tableName, LINK_COL, clientId, null);
        if (isEmpty(linkCol) || "-".equals(linkCol)) {
            log.warning("[WFDetail] " + tableName
                    + ": LINE_TABLE=" + lineTable
                    + " is configured but LINK_COL is empty or '-'."
                    + " Lines will not be displayed.");
        }

        String col2Cfg = getSysConfig(tableName, COL2, clientId, null);
        if (col2Cfg == null)
            log.info("[WFDetail] " + tableName
                    + ": COL2 is not configured, using default: " + DEFAULT_COL2);

        String col3Cfg = getSysConfig(tableName, COL3, clientId, null);
        if (col3Cfg == null)
            log.info("[WFDetail] " + tableName
                    + ": COL3 is not configured, using default: " + DEFAULT_COL3);

        String orderBy = getSysConfig(tableName, ORDER_BY, clientId, null);
        if (orderBy == null)
            log.info("[WFDetail] " + tableName
                    + ": ORDER_BY not configured, fallback to LINK_COL=" + linkCol);
    }

    // =========================================================================
    // PRIVATE — SYSCONFIG
    // =========================================================================

    private String getSysConfig(String tableName, String suffix,
                                int clientId, String defaultValue) {
        String key   = PREFIX + tableName + suffix;
        String value = MSysConfig.getValue(key, null, clientId);
        return (value != null && !value.trim().isEmpty())
                ? value.trim()
                : defaultValue;
    }

    // =========================================================================
    // PRIVATE — LABEL RESOLUTION
    // =========================================================================

    /**
     * Resolve label with priority:
     * 1. AD_Element PrintName (auto, follows user language)
     * 2. SysConfig "_xxx_LABEL" (manual admin override)
     * 3. DEFAULT_*_LABEL (hardcoded fallback)
     * 4. "-" (nothing found)
     *
     * If admin sets SysConfig "_xxx_LABEL" = "-", label is hidden (returns "").
     */
    private String getLabel(String tableName, String labelSuffix, int clientId,
                            String columnDefs, String defaultLabel) {

        String cfg = getSysConfig(tableName, labelSuffix, clientId, null);

        // Explicit hide command
        if ("-".equals(cfg)) return "";

        // 1. Auto from AD_Element PrintName (follows user language)
        String auto = resolveColumnLabel(tableName, columnDefs);
        if (!isEmpty(auto)) return auto;

        // 2. Manual override from SysConfig
        if (!isEmpty(cfg)) return cfg;

        // 3. Hardcoded default
        if (!isEmpty(defaultLabel)) return defaultLabel;

        // 4. Nothing found
        return "-";
    }

    /**
     * Resolve label from AD_Element (via MColumn.PrintName) based on
     * the first valid column candidate in columnDefs.
     * Supports FK chain "X_ID>Y" — label taken from column Y on FK target table X.
     */
    private String resolveColumnLabel(String tableName, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : splitColumnDefs(columnDefs)) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;

            try {
                String targetTable = tableName;
                String targetCol   = candidate;

                if (candidate.contains(">")) {
                    String[] segs = candidate.split(">");
                    String fkCol  = segs[0].trim();
                    targetCol     = segs[segs.length - 1].trim();
                    targetTable   = fkCol.endsWith("_ID")
                            ? fkCol.substring(0, fkCol.length() - 3)
                            : fkCol;
                }

                MColumn col = MColumn.get(Env.getCtx(), targetTable, targetCol);
                if (col == null) continue;

                // Ambil PrintName langsung dari translation MColumn
                String label = col.get_Translation("PrintName");

                // Fallback ke Name jika PrintName kosong
                if (isEmpty(label))
                    label = col.get_Translation("Name");

                // Fallback ke ColumnName jika masih kosong
                if (isEmpty(label))
                    label = col.getColumnName();

                if (!isEmpty(label)) return label.trim();

            } catch (Exception e) {
                log.fine("[WFDetail] resolveColumnLabel failed for '"
                        + candidate + "': " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get display name of table from AD_Table (translated).
     */
    private String getTableDisplayName(String tableName) {
        try {
            MTable table = MTable.get(Env.getCtx(), tableName);
            if (table == null) return tableName;
            String name = table.get_Translation("Name");
            if (isEmpty(name)) name = table.getName();
            return isEmpty(name) ? tableName : name;
        } catch (Exception e) {
            return tableName;
        }
    }

    // =========================================================================
    // PRIVATE — COLUMN RESOLUTION
    // =========================================================================

    private String resolveColumnValue(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : splitColumnDefs(columnDefs)) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;

            String value = resolveOneColumn(po, candidate);
            if (value != null && !value.trim().isEmpty())
                return value.trim();
        }
        return null;
    }

    private List<String> splitColumnDefs(String columnDefs) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideFKChain = false;

        for (int i = 0; i < columnDefs.length(); i++) {
            char c = columnDefs.charAt(i);

            if (c == '>') {
                insideFKChain = true;
                current.append(c);
            } else if (c == ',') {
                if (insideFKChain) {
                    insideFKChain = false;
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0)
            result.add(current.toString());

        return result;
    }

    private Object resolveColumnObject(PO po, String columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) return null;

        for (String candidate : splitColumnDefs(columnDefs)) {
            candidate = candidate.trim();
            if (candidate.isEmpty() || "-".equals(candidate)) continue;
            if (candidate.contains(">")) continue;

            Object val = po.get_Value(candidate);
            if (val != null) return val;
        }
        return null;
    }

    private String resolveOneColumn(PO po, String columnDef) {
        if (columnDef == null || columnDef.isEmpty()) return null;

        if (!columnDef.contains(">")) {
            Object val = po.get_Value(columnDef);
            return val != null ? val.toString().trim() : null;
        }

        String[] segments  = columnDef.split(">");
        String   targetCol = segments[segments.length - 1].trim();
        PO       currentPO = po;

        for (int i = 0; i < segments.length - 1; i++) {
            String fkCol = segments[i].trim();

            int fkId = currentPO.get_ValueAsInt(fkCol);
            if (fkId <= 0) {
                log.fine("[WFDetail] FK is 0/null in segment '"
                        + fkCol + "' in chain '" + columnDef + "'");
                return null;
            }

            String fkTableName = fkCol.endsWith("_ID")
                    ? fkCol.substring(0, fkCol.length() - 3)
                    : fkCol;

            try {
                MTable fkTable = MTable.get(Env.getCtx(), fkTableName);
                if (fkTable == null) {
                    log.warning("[WFDetail] FK table not found: '"
                            + fkTableName + "' (segment '" + fkCol
                            + "' in chain '" + columnDef + "')");
                    return null;
                }

                PO fkPO = fkTable.getPO(fkId, null);
                if (fkPO == null) {
                    log.warning("[WFDetail] Record not found: "
                            + fkTableName + " #" + fkId);
                    return null;
                }

                currentPO = fkPO;

            } catch (Exception e) {
                log.warning("[WFDetail] FK lookup failed [segment=" + fkCol
                        + ", chain=" + columnDef + "]: " + e.getMessage());
                return null;
            }
        }

        Object val = currentPO.get_Value(targetCol);
        return val != null ? val.toString().trim() : null;
    }

    // =========================================================================
    // PRIVATE — FORMATTING
    // =========================================================================

    private String formatByType(String raw, String type, String context) {
        if (isEmpty(raw) || "-".equals(raw))
            return TYPE_STRING.equals(type) ? "-" : "0";

        if (TYPE_STRING.equals(type))
            return raw;

        if (!isNumericString(raw)) {
            log.warning("[WFDetail] " + context
                    + ": value '" + raw + "' not numeric."
                    + " Check Configuration _TYPE or column name in SysConfig."
                    + " Displayed as '0'.");
            return "0";
        }

        return formatAmount(raw, context);
    }

    private boolean isNumericString(String value) {
        if (isEmpty(value)) return false;
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

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
            log.warning("[WFDetail] formatAmount failed for " + context
                    + ": value='" + rawValue + "' is non-numeric, displayed as-is.");
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
            log.warning("[WFDetail] formatDate failed: " + e.getMessage());
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
            log.warning("[WFDetail] resolveCreatedByName failed: " + e.getMessage());
        }
        return "-";
    }

    // =========================================================================
    // PRIVATE — UI HELPERS
    // =========================================================================

    private void setLabelWithTitle(Label lValue, Label lTitle,
                                   String value, String title) {
        if (lValue != null)
            lValue.setValue(isEmpty(value) ? "-" : value);
        if (lTitle != null)
            lTitle.setValue(isEmpty(title) || "-".equals(title) ? "" : title);
    }

    private void clearUI() {
        historyHandler.close();
        List<Listitem> toRemove = new ArrayList<>(lstTxLines.getItems());
        for (Listitem item : toRemove)
            lstTxLines.removeChild(item);

        lstTxLines.setVisible(false);

        setLabelWithTitle(lHdrCol1, lHdrCol1Title, "-", "");
        setLabelWithTitle(lHdrCol2, lHdrCol2Title, "-", "");
        setLabelWithTitle(lHdrCol3, lHdrCol3Title, "-", "");
        setLabelWithTitle(lHdrCol4, lHdrCol4Title, "-", "");

        setGroupboxCaption("Transaction Detail");
        grpTxDetails.setVisible(false);
    }

    private void setGroupboxCaption(String text) {
        org.zkoss.zul.Caption existing = grpTxDetails.getCaption();
        if (existing != null)
            grpTxDetails.removeChild(existing);

        org.zkoss.zul.Caption cp = new org.zkoss.zul.Caption(text);
        grpTxDetails.appendChild(cp);
    }

    private void appendInfoRow(String message) {
        Listitem item = new Listitem();
        Listcell cell = new Listcell(message);
        cell.setSpan(3); // colspan 3 untuk 3 kolom — fix dari setSpan(true) yang error
        item.appendChild(cell);
        lstTxLines.appendChild(item);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
