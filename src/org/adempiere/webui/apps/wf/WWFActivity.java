/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * Copyright (C) 2008 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.webui.apps.wf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.Icon;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MRefList;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.model.SystemIDs;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MBPartner;
import org.zkoss.zul.West;
import org.zkoss.zul.Listitem;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFProcess;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.adempiere.webui.component.FlexHlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Groupbox;
import java.lang.reflect.Method;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabpanel;

/**
 * Workflow activity form
 * @author hengsin
 *
 */
@org.idempiere.ui.zk.annotation.Form(name = "org.nsoft.webui.apps.wf.WFActivity")
public class WWFActivity extends ADForm implements EventListener<Event>
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -1658595186719510159L;
	/**	Open Activities				*/
	private MWFActivity[] 		m_activities = null;
	/**	Current Activity			*/
	private MWFActivity 		m_activity = null;
	/**	Current Activity			*/
	private int	 				m_index = 0;
	/**	Set Column					*/
	private	MColumn 			m_column = null;
	/**	Logger			*/
	private static final CLogger log = CLogger.getCLogger(WWFActivity.class);

	//
	private Label lNode = new Label(Msg.translate(Env.getCtx(), "AD_WF_Node_ID"));
	private Textbox fNode = new Textbox();
	private Label lDesctiption = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Textbox fDescription = new Textbox();
	private Label lHelp = new Label(Msg.translate(Env.getCtx(), "Help"));
	private Textbox fHelp = new Textbox();
	private Label lHistory = new Label(Msg.translate(Env.getCtx(), "History"));
	private Html fHistory = new Html();
	private Label lAnswer = new Label(Msg.getMsg(Env.getCtx(), "Answer"));
	private Textbox fAnswerText = new Textbox();
	private Listbox fAnswerList = new Listbox();
	private Button fAnswerButton = new Button();
	private Button bZoom = new Button();
	private Label lTextMsg = new Label(Msg.getMsg(Env.getCtx(), "Messages"));
	private Textbox fTextMsg = new Textbox();
	private Button bOK = new Button();
	private WSearchEditor fForward = null;	//	dynInit
	private Label lForward = new Label(Msg.getMsg(Env.getCtx(), "Forward") + " (" + Msg.translate(Env.getCtx(), "Optional") + ")");
	private StatusBarPanel statusBar = new StatusBarPanel();
	private Button bRefresh = new Button();

	private ListModelTable model = null;
	private WListbox listbox = new WListbox();		
	private Groupbox grpTxDetails = new Groupbox();
	private Listbox lstTxLines = new Listbox();
	private	Label titleWestpanel = new Label(Msg.getMsg(Env.getCtx(),"List Approval"));
    private Label titleNode = new Label(Msg.getMsg(Env.getCtx(), "Approval Node"));
	private	Label titleTablines = new Label(Msg.getMsg(Env.getCtx(),"Approval Detail"));
	private	Label titleHeaderlines = new Label(Msg.getMsg(Env.getCtx(),"Header & Lines"));
	private	Label titleAction = new Label(Msg.getMsg(Env.getCtx(),"Approval Action"));
	private	Label titleApprove = new Label(Msg.getMsg(Env.getCtx(),"Approve"));
	private	Label titleReject = new Label(Msg.getMsg(Env.getCtx(),"Reject"));
	private final static String HISTORY_DIV_START_TAG = "<div style='overflow-y:scroll;height: 100px; border: 1px solid #7F9DB9;'>";
	private Label lHdrDocNo      = new Label();
	private Label lHdrDateDoc    = new Label();
	private Label lHdrBPName     = new Label();
	private Label lHdrGrandTotal = new Label();
	private Tabbox tabboxDetail = new Tabbox();
	private Tabpanels tabpanels = new Tabpanels();

	private FlexHlayout createModernActionButtons() {
		FlexHlayout buttonLayout = new FlexHlayout();
		buttonLayout.setHflex("1");
		buttonLayout.setSpacing("15px");
		buttonLayout.setStyle("margin-top: 20px; padding: 10px 0; justify-content: flex-end;");

		// Tombol Approve (Hijau)
		Button btnApprove = new Button("Approve");
		btnApprove.setHeight("45px");
		btnApprove.setHflex("1");
		btnApprove.setStyle("background-color: #2ecc71; color: white; font-weight: bold; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;");
		btnApprove.addEventListener(Events.ON_CLICK, e -> executeApprovalDirectly(true));

		// Tombol Reject (Merah)
		Button btnReject = new Button("Reject");
		btnReject.setHeight("45px");
		btnReject.setHflex("1");
		btnReject.setStyle("background-color: #e74c3c; color: white; font-weight: bold; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;");
		btnReject.addEventListener(Events.ON_CLICK, e -> executeApprovalDirectly(false));

		buttonLayout.appendChild(btnReject);
		buttonLayout.appendChild(btnApprove);
		
		return buttonLayout;
	}

	/**
	 * default constructor
	 */
	public WWFActivity()
	{
		super();
		LayoutUtils.addSclass("workflow-activity-form", this);
	}

	/**
	 * Load activities and layout form
	 */
    protected void initForm()
    {
        loadActivities();

        fAnswerList.setMold("select");

		if (ThemeManager.isUseFontIconForImage()) {
        	bZoom.setIconSclass(Icon.getIconSclass(Icon.ZOOM));
        	bOK.setIconSclass(Icon.getIconSclass(Icon.OK));
			bRefresh.setIconSclass(Icon.getIconSclass(Icon.REFRESH));
        } else {
        	bZoom.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
        	bOK.setImage(ThemeManager.getThemeResource("images/Ok16.png"));
			bRefresh.setImage(ThemeManager.getThemeResource("images/Refresh16.png"));
        }
		setTooltipText(bZoom, "Zoom");
		setTooltipText(bOK, "Ok");
		setTooltipText(bRefresh, "Refresh");

        MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,
                0, SystemIDs.COLUMN_AD_WF_ACTIVITY_AD_USER_ID, DisplayType.Search);
        fForward = new WSearchEditor(lookup, Msg.translate(
                Env.getCtx(), "AD_User_ID"), "", true, false, true);

        init();
        display(-1);
    }

    /**
     * set tooltip text of btn
     * @param btn
     * @param key AD_Message key
     */
	private void setTooltipText(Button btn, String key) {
		String text = Util.cleanAmp(Msg.translate(Env.getCtx(), key));
		if (!Util.isEmpty(text, true))
			btn.setTooltiptext(text);
	}

	/**
	 * Layout form
	 */
	private void init()
	{
		// Part 1: West Panel (Approval List)
		West westPanel = new West();
		westPanel.setSize("320px");
		westPanel.setSplittable(true);
		westPanel.setCollapsible(true);
		westPanel.setTitle("Approval List");
		
		// The listbox is configured so that its items stack neatly.
		listbox.setSclass("wf-approval-listbox");
		ZKUpdateUtil.setVflex(listbox, "1");
		ZKUpdateUtil.setHflex(listbox, "1");
		westPanel.appendChild(listbox);
		listbox.addEventListener(Events.ON_SELECT, this);

		// Part 2: Center Panel - Node Aproval
		Vlayout nodeApprovalArea = new Vlayout();
		nodeApprovalArea.setHflex("1");
		nodeApprovalArea.setSpacing("8px");
		nodeApprovalArea.setStyle("background: #ffffff; padding: 15px; border: 1px solid #e2e8f0; border-radius: 8px;");

		//Part 2.1: Title Section
		titleNode.setStyle("font-weight: bold; font-size: 14px; color: #2d3748; display: block; margin-bottom: 5px;");
		nodeApprovalArea.appendChild(titleNode);

		//Part 2.2: Node Field Group
		Vlayout nodeGroup = new Vlayout();
		nodeGroup.setSpacing("3px");
		lNode.setStyle("font-weight: 600; color: #4a5568; font-size: 12px;");
		nodeGroup.appendChild(lNode);
		nodeGroup.appendChild(fNode);
		ZKUpdateUtil.setHflex(fNode, "true");
		fNode.setReadonly(true);
		fNode.setStyle("background: #f7fafc; border: 1px solid #cbd5e0; padding: 6px; border-radius: 4px;");
		nodeApprovalArea.appendChild(nodeGroup);

		//Part 2.3: Description Field
		Vlayout descGroup = new Vlayout();
		descGroup.setSpacing("3px");
		lDesctiption.setStyle("font-weight: 600; color: #4a5568; font-size: 12px;");
		descGroup.appendChild(lDesctiption);
		descGroup.appendChild(fDescription);
		ZKUpdateUtil.setHflex(fDescription, "true");
		fDescription.setMultiline(true);
		fDescription.setReadonly(true);
		fDescription.setStyle("background: #f7fafc; border: 1px solid #cbd5e0; padding: 6px; border-radius: 4px; min-height: 40px;");
		nodeApprovalArea.appendChild(descGroup);

		//Part 2.4: Help Field 
		Vlayout helpGroup = new Vlayout();
		helpGroup.setSpacing("3px");
		lHelp.setStyle("font-weight: 600; color: #4a5568; font-size: 12px;");
		helpGroup.appendChild(lHelp);
		helpGroup.appendChild(fHelp);
		ZKUpdateUtil.setHflex(fHelp, "true");
		fHelp.setMultiline(true);
		fHelp.setRows(2);
		fHelp.setReadonly(true);
		fHelp.setStyle("background: #f7fafc; border: 1px solid #cbd5e0; padding: 6px; border-radius: 4px; font-style: italic; color: #718096;");
		nodeApprovalArea.appendChild(helpGroup);

    // Part 3.1: Struktur Tabbox Utama
    Tabs tabs = new Tabs();
    tabs.appendChild(new Tab("Detail Transaksi")); // Tab indeks 0
    tabs.appendChild(new Tab("Riwayat"));          // Tab indeks 1
    tabboxDetail.appendChild(tabs);       

    // Pastikan variabel tabpanels sudah di-instansiasi sebelum di-append
    // tabpanels = new Tabpanels(); 
    tabboxDetail.appendChild(tabpanels);  

    // Part 3.2: Membuat Tabpanel 1 - Detail Transaksi
    Tabpanel panelLines = new Tabpanel();

    grpTxDetails = new Groupbox();
    grpTxDetails.setCaption("Header Doc");
    grpTxDetails.setOpen(true);
    grpTxDetails.setHflex("1");
    grpTxDetails.setVisible(true); 
    grpTxDetails.setStyle("border: none; padding: 0;");

    // --- Layout Grid untuk Header ---
    Grid headerGrid = new Grid();
    headerGrid.setStyle("border: none; margin-bottom: 10px;");
    Columns columns = new Columns();
    columns.appendChild(new Column());
    columns.appendChild(new Column());
    headerGrid.appendChild(columns);

    Rows rows = new Rows();

    // Baris 1: No Dokumen
    Row row1 = new Row();
    row1.appendChild(new Label("Doc No:"));
    row1.appendChild(lHdrDocNo);
    rows.appendChild(row1);

     // Baris 2: Tanggal
     Row row2 = new Row();
     row2.appendChild(new Label("Date:"));
     row2.appendChild(lHdrDateDoc);
     rows.appendChild(row2);

      // Baris 3: Nama Partner
      Row row3 = new Row();
      row3.appendChild(new Label("BP Name:"));
      row3.appendChild(lHdrBPName);
      rows.appendChild(row3);

      // Baris 4: Grand Total
      Row row4 = new Row();
      row4.appendChild(new Label("Grand Total:"));
      row4.appendChild(lHdrGrandTotal);
      rows.appendChild(row4);

      headerGrid.appendChild(rows);
      grpTxDetails.appendChild(headerGrid); // Masukkan grid ke groupbox

       // --- Layout Listbox untuk Lines ---
       lstTxLines = new Listbox();
       lstTxLines.setHflex("1");
       lstTxLines.setSpan(true);
       lstTxLines.setSclass("mobile-scrollable-list");

       Listhead listHead = new Listhead();
       listHead.appendChild(createHeader("Description", "2")); 
       listHead.appendChild(createHeader("Qty", "1"));         
       listHead.appendChild(createHeader("Total", "1"));       
       lstTxLines.appendChild(listHead);

       grpTxDetails.appendChild(lstTxLines); // Masukkan listbox ke groupbox
       panelLines.appendChild(grpTxDetails); // Masukkan semua isi groupbox ke panelLines
       // Part 3.3: Membuat Tabpanel 2 - History
       Tabpanel panelHistory = new Tabpanel();
       tabpanels.appendChild(panelLines);    // Masuk ke Tab "Detail Transaksi"
       tabpanels.appendChild(panelHistory);  // Masuk ke Tab "Riwayat"

		Vlayout historyLayout = new Vlayout();
		historyLayout.setHflex("1");
		historyLayout.setSpacing("5px");
		lHistory.setStyle("font-weight: bold; color: #2d3748;");
		historyLayout.appendChild(lHistory);
		historyLayout.appendChild(fHistory);
		ZKUpdateUtil.setHflex(fHistory, "true");
		fHistory.setStyle("border: 1px solid #cbd5e0; border-radius: 4px; padding: 6px;");
		panelHistory.appendChild(historyLayout);

		//Part 4: Center Panel - Approval Action(Footer)
		Vlayout footerApprovalArea = new Vlayout();
		footerApprovalArea.setHflex("1");
		footerApprovalArea.setSpacing("12px");
		footerApprovalArea.setStyle("background: #ffffff; padding: 15px; border: 1px solid #e2e8f0; border-radius: 8px; margin-top: 10px;");

		//Part 4.1: Footer Title Section
		titleAction.setStyle("font-weight: bold; font-size: 14px; color: #2d3748; display: block;");
		footerApprovalArea.appendChild(titleAction);

		//Part 4.2: txtMessage Group
		Vlayout msgGroup = new Vlayout();
		msgGroup.setSpacing("5px");
		lTextMsg.setStyle("font-weight: 600; color: #4a5568; font-size: 12px;");
		msgGroup.appendChild(lTextMsg);
		msgGroup.appendChild(fTextMsg);
		fTextMsg.setMultiline(true);
		fTextMsg.setRows(3);
		ZKUpdateUtil.setWidth(fTextMsg, "100%");
		fTextMsg.setStyle("border: 1px solid #cbd5e0; padding: 8px; border-radius: 4px; font-family: sans-serif;");
		footerApprovalArea.appendChild(msgGroup);

		//Part 4.3: Answer Row
		FlexHlayout answerRow = new FlexHlayout();
		answerRow.setHflex("1");
		answerRow.setValign("middle");
		lAnswer.setStyle("font-weight: 600; color: #4a5568; font-size: 12px; margin-right: 5px;");
		answerRow.appendChild(lAnswer);
		answerRow.appendChild(fAnswerText);
		ZKUpdateUtil.setHflex(fAnswerText, "true");
		answerRow.appendChild(fAnswerList);
		answerRow.appendChild(fAnswerButton);
		answerRow.appendChild(bZoom);
		footerApprovalArea.appendChild(answerRow);
		fAnswerButton.addEventListener(Events.ON_CLICK, this);
		bZoom.addEventListener(Events.ON_CLICK, this);

		//Part 4.4: Forward Row
		Vlayout forwardSection = new Vlayout();
		forwardSection.setSpacing("5px");
		lForward.setStyle("font-weight: 600; color: #4a5568; font-size: 12px;");
		forwardSection.appendChild(lForward);
		
		FlexHlayout forwardActions = new FlexHlayout();
		forwardActions.setHflex("1");
		forwardActions.setValign("middle");
		forwardActions.appendChild(fForward.getComponent());
		forwardActions.appendChild(bOK);
		forwardActions.appendChild(bRefresh);
		bOK.addEventListener(Events.ON_CLICK, this);
		bRefresh.addEventListener(Events.ON_CLICK, this);
		forwardSection.appendChild(forwardActions);
		
		footerApprovalArea.appendChild(forwardSection);

		//Part 4.5: Approve and reject button
		FlexHlayout mainActionButtons = new FlexHlayout();
		mainActionButtons.setHflex("1");
		mainActionButtons.setSpacing("15px");
		
		//Use function createModernActionButtons()
		FlexHlayout customButtons = createModernActionButtons();
		mainActionButtons.appendChild(customButtons);
		ZKUpdateUtil.setHflex(customButtons, "1");
		
		footerApprovalArea.appendChild(mainActionButtons);

		//Part 5: Main layout - mainChatlayout
		Borderlayout mainChatLayout = new Borderlayout();
		ZKUpdateUtil.setWidth(mainChatLayout, "100%");
		ZKUpdateUtil.setHeight(mainChatLayout, "100%");
		mainChatLayout.setStyle("background-color: #f7f9fa; position: relative;");

		//Part 5.1: Put westPanel (List Approval)
		mainChatLayout.appendChild(westPanel);

		//Part 5.2: Put centerPanel(Detail Area: Node, Detail and Action)
		Center centerPanel = new Center();
		centerPanel.setStyle("background-color: transparent; overflow: auto;");

		Vlayout chatAreaLayout = new Vlayout();
		chatAreaLayout.setHflex("1");
		chatAreaLayout.setSpacing("15px");
		chatAreaLayout.setStyle("padding: 15px;");

		//Part 5.3: Sort by Node, Detail Approval and Approval Action
		chatAreaLayout.appendChild(nodeApprovalArea);   // 1. Node Approval Section
		chatAreaLayout.appendChild(tabboxDetail);       // 2. Tab Details & History
		chatAreaLayout.appendChild(footerApprovalArea);  // 3. Footer - Approval action

		centerPanel.appendChild(chatAreaLayout);
		mainChatLayout.appendChild(centerPanel);

		// Status Bar
		South south = new South();
		south.appendChild(statusBar);
		south.setStyle("background-color: transparent;");
		mainChatLayout.appendChild(south);

		this.appendChild(mainChatLayout);
		this.setStyle("height: 100%; width: 100%; position: relative;");
	}

	private void renderTransactionDetails(MWFActivity activity) {
		// 1. Reset state view awal
		lstTxLines.getItems().clear(); // Cukup hapus item-nya saja, jangan clear total agar Listhead di init() tidak hilang
		grpTxDetails.setVisible(true);
		lHdrDocNo.setValue("-");
		lHdrDateDoc.setValue("-");
		lHdrBPName.setValue("-");
		lHdrGrandTotal.setValue("-");

		if (activity == null || activity.getRecord_ID() <= 0) return;

		try {
			int tableId = activity.getAD_Table_ID();
			MTable table = MTable.get(Env.getCtx(), tableId);
			PO headerPO = table.getPO(activity.getRecord_ID(), null);
			
			if (headerPO == null) return;

			// Tampilkan container groupbox karena dokumen ditemukan
			grpTxDetails.setVisible(true);

			// Set Nilai Header Component
			lHdrDocNo.setValue(getFieldValue(headerPO, "getDocumentNo"));
			Object rawDate = activity.getCreated();
			lHdrDateDoc.setValue(formatDate(rawDate));
			lHdrBPName.setValue(getBPName(headerPO));
			
			String grandTotal = getFieldValue(headerPO, "getGrandTotal", "getTotalLines");
			if ("-".equals(grandTotal)) {
				grandTotal = calcTotalFromLines(headerPO);
			}
			lHdrGrandTotal.setValue(grandTotal);

			// 2. Ambil data Lines menggunakan Java Reflection
			Method getLinesMethod = null;
			try {
				getLinesMethod = headerPO.getClass().getMethod("getLines");
			} catch (NoSuchMethodException e) {
				// Jika dokumen tidak punya baris/lines (misal dokumen cetak cetak berseri tertentu), 
				// biarkan header tetap tampil namun jangan teruskan ke loop lines.
				log.warning("Dokumen " + headerPO.get_TableName() + " tidak memiliki metode getLines().");
				return; 
			}

			Object[] lines = (Object[]) getLinesMethod.invoke(headerPO);
			if (lines != null && lines.length > 0) {
				
				// Catatan: Pastikan komponen `Listhead` sudah Anda tempel sekali saja di metode init() 
				// Contoh di init(): lstTxLines.appendChild(listHead);

				for (Object line : lines) {
					Listitem item = new Listitem();
					
					// Ambil Nama Produk / Deskripsi
					String itemDetail = getProductName(line);
					
					// Ambil Quantity Dinamis
					String qty = "0";
					String[] qtyMethodNames = {"getQtyInvoiced", "getQtyOrdered", "getQtyEntered", "getQtyField", "getQtyDelivered", "getQty"};
					for (String methodName : qtyMethodNames) {
						try {
							Method getQty = line.getClass().getMethod(methodName);
							Object qtyObj = getQty.invoke(line);
							if (qtyObj != null) {
								qty = qtyObj.toString();
								break;
							}
						} catch (Exception ignored) {}
					}

					// Ambil Nilai Total Line Dinamis
					String lineNetAmt = "0";
					try {
						Method getLineNetAmt = line.getClass().getMethod("getLineNetAmt");
						Object amtObj = getLineNetAmt.invoke(line);
						if (amtObj != null) lineNetAmt = amtObj.toString();
					} catch (Exception e) {
						try {
							Method getPriceActual = line.getClass().getMethod("getPriceActual");
							Object priceObj = getPriceActual.invoke(line);
							if (priceObj != null) lineNetAmt = priceObj.toString();
						} catch (Exception ignored) {}
					}

					// Tambah data ke dalam baris tabel ZK
					item.appendChild(new Listcell(itemDetail));
					item.appendChild(new Listcell(qty));
					item.appendChild(new Listcell(lineNetAmt));
					lstTxLines.appendChild(item);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Gagal memproses renderTransactionDetails kustom", e);
		}
	}

	/**
	 * Coba beberapa method getter, return nilai pertama yang tidak null/kosong.
	 * Fallback akhir: "-"
	 */
	private String getFieldValue(Object obj, String... methodNames) {
		for (String methodName : methodNames) {
			try {
				Method m   = obj.getClass().getMethod(methodName);
				Object val = m.invoke(obj);
				if (val != null && !val.toString().trim().isEmpty())
					return val.toString();
			} catch (Exception ignored) {}
		}
		return "-";
	}
	 
	/**
	 * Ambil nama BP: getC_BPartner().getName()
	 * Fallback: getC_BPartner_ID lalu query MBPartner langsung
	 */
	private String getBPName(PO headerPO) {
		// Coba via object relasi
		try {
			Method getBP = headerPO.getClass().getMethod("getC_BPartner");
			Object bp    = getBP.invoke(headerPO);
			if (bp != null) {
				Method getName = bp.getClass().getMethod("getName");
				Object name    = getName.invoke(bp);
				if (name != null && !name.toString().trim().isEmpty())
					return name.toString();
			}
		} catch (Exception ignored) {}
	 
		// Fallback: ambil C_BPartner_ID lalu query tabel langsung
		try {
			Method getIdMethod = headerPO.getClass().getMethod("getC_BPartner_ID");
			Object bpId        = getIdMethod.invoke(headerPO);
			if (bpId != null && (Integer) bpId > 0) {
				org.compiere.model.MBPartner bp =
					new org.compiere.model.MBPartner(Env.getCtx(), (Integer) bpId, null);
				if (bp.getName() != null) return bp.getName();
			}
		} catch (Exception ignored) {}
	 
		return "-";
	}
	 
	/**
	 * Ambil nama produk dari line: M_Product.getName() → getDescription() → "Item"
	 */
	private String getProductName(Object line) {
		try {
			Method getProduct = line.getClass().getMethod("getM_Product");
			Object product    = getProduct.invoke(line);
			if (product != null) {
				Method getName = product.getClass().getMethod("getName");
				Object name    = getName.invoke(product);
				if (name != null && !name.toString().trim().isEmpty())
					return name.toString();
			}
		} catch (Exception ignored) {}
	 
		String desc = getFieldValue(line, "getDescription", "getName");
		return "-".equals(desc) ? "Item" : desc;
	}
	 
	/**
	 * Kalkulasi total manual dari lines jika getGrandTotal/getTotalLines tidak tersedia.
	 * Menjumlahkan getLineNetAmt tiap line, fallback ke getPriceActual * Qty.
	 */
	private String calcTotalFromLines(PO headerPO) {
		try {
			Method getLinesMethod = headerPO.getClass().getMethod("getLines");
			Object[] lines        = (Object[]) getLinesMethod.invoke(headerPO);
	 
			if (lines == null || lines.length == 0)
				return "-";
	 
			java.math.BigDecimal total = java.math.BigDecimal.ZERO;
			for (Object line : lines) {
				// Coba getLineNetAmt dulu
				try {
					Method m   = line.getClass().getMethod("getLineNetAmt");
					Object val = m.invoke(line);
					if (val instanceof java.math.BigDecimal)
						total = total.add((java.math.BigDecimal) val);
					continue;
				} catch (Exception ignored) {}
	 
				// Fallback: PriceActual * Qty
				try {
					Method mPrice = line.getClass().getMethod("getPriceActual");
					Method mQty   = line.getClass().getMethod("getQtyOrdered");
					Object price  = mPrice.invoke(line);
					Object qty    = mQty.invoke(line);
					if (price instanceof java.math.BigDecimal && qty instanceof java.math.BigDecimal)
						total = total.add(
							((java.math.BigDecimal) price)
								.multiply((java.math.BigDecimal) qty));
				} catch (Exception ignored) {}
			}
	 
			// Format dengan 2 desimal
			return total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
	 
		} catch (Exception e) {
			log.warning("calcTotalFromLines gagal: " + e.getMessage());
			return "-";
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
			else
				return dateObj.toString();

			// Ambil locale dari Language iDempiere — mengikuti setup system/user
			org.compiere.util.Language lang = Env.getLanguage(Env.getCtx());
			java.util.Locale locale = lang.getLocale();

			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
				"dd MMMM yyyy", locale
			);
			return sdf.format(date);

		} catch (Exception e) {
			return dateObj.toString();
		}
	}
	private Listheader createHeader(String label, String ratio) {
        Listheader header = new Listheader(label);
        header.setHflex(ratio); 
        return header;
    }

	private void executeApprovalDirectly(boolean isApproved) {
		if (m_activity == null) return;

		try {
			// 1. Deteksi komponen input jawaban bawaan iDempiere
			if (fAnswerList.isVisible() && fAnswerList.getItemCount() > 0) {
				// Jika tipe datanya Yes/No (1 untuk Yes/Approve, 0 untuk No/Reject)
				if (isApproved) {
					fAnswerList.setSelectedIndex(0); // Biasanya indeks 0 adalah 'Yes' / 'Approve'
				} else {
					fAnswerList.setSelectedIndex(1); // Biasanya indeks 1 adalah 'No' / 'Reject'
				}
			} else if (fAnswerText.isVisible()) {
				// Jika tipenya isian teks bebas
				fAnswerText.setText(isApproved ? "Approved" : "Rejected");
			}

			// 2. Kirim catatan evaluasi dari kolom pesan (fTextMsg) jika diisi user
			
			// 3. Picu secara keras Event klik tombol OK bawaan iDempiere
			org.zkoss.zk.ui.event.Event clickEvent = new org.zkoss.zk.ui.event.Event(Events.ON_CLICK, bOK);
			org.zkoss.zk.ui.event.Events.sendEvent(bOK, clickEvent);
			
		} catch (Exception ex) {
			log.log(java.util.logging.Level.SEVERE, "Gagal mengeksekusi aksi tombol persetujuan kustom", ex);
		}
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();
        String eventName = event.getName();

        if(eventName.equals(Events.ON_CLICK))
        {
    		if (comp == bZoom)
    			cmd_zoom();
    		else if (comp == bRefresh)
    		{
    			Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
    			Executions.schedule(getDesktop(), e -> {
    				loadActivities();
    				Clients.clearBusy();
    			}, new Event("onRefresh"));
    		}
    		else if (comp == bOK)
    		{
    			Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
    			Events.echoEvent("onOK", this, null);
    		}
    		else if (comp == fAnswerButton)
    			cmd_button();
        } 
        else if (Events.ON_SELECT.equals(eventName) && comp == listbox)
        {
        	m_index = listbox.getSelectedIndex();
        	if (m_index >= 0)
    			display(m_index);
        }
        else
        {
    		super.onEvent(event);
        }
	}

	/**
	 * Get active activities count
	 * @return pending activities count
	 */
	public int getActivitiesCount()
	{
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		int count = new Query(Env.getCtx(), MWFActivity.Table_Name, MWFActivity.getWhereUserPendingActivities(), null)
				.setApplyAccessFilter(true, false)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_Client_ID)
				.count();
		return count;
	}

	/**
	 * 	Load Activities
	 * 	@return number of activities loaded
	 */
	public int loadActivities()
	{
		long start = System.currentTimeMillis();

		int MAX_ACTIVITIES_IN_LIST = MSysConfig.getIntValue(MSysConfig.MAX_ACTIVITIES_IN_LIST, 200, Env.getAD_Client_ID(Env.getCtx()));

		model = new ListModelTable();

		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		Iterator<MWFActivity> it = new Query(Env.getCtx(), MWFActivity.Table_Name, MWFActivity.getWhereUserPendingActivities(), null)
				.setApplyAccessFilter(true, false)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_Client_ID)
				.setOrderBy("AD_WF_Activity.Priority DESC, AD_WF_Activity.Created")
				.iterate();

		List<MWFActivity> list = new ArrayList<MWFActivity>();
		while (it.hasNext()) {
			MWFActivity activity = it.next();
			list.add (activity);
			List<Object> rowData = new ArrayList<Object>();
			rowData.add(activity.getPriority());
			rowData.add(activity.getNodeName());
			rowData.add(activity.getSummary());
			model.add(rowData);
			if (list.size() > MAX_ACTIVITIES_IN_LIST && MAX_ACTIVITIES_IN_LIST > 0)
			{
				log.warning("More than " + MAX_ACTIVITIES_IN_LIST + " Activities - ignored");
				break;
			}
		}
		m_activities = new MWFActivity[list.size ()];
		list.toArray (m_activities);
		//
		if (log.isLoggable(Level.FINE)) log.fine("#" + m_activities.length
			+ "(" + (System.currentTimeMillis()-start) + "ms)");
		m_index = 0;

		String[] columns = new String[]{Msg.translate(Env.getCtx(), "Priority"),
				Msg.translate(Env.getCtx(), "AD_WF_Node_ID"),
				Msg.translate(Env.getCtx(), "Summary")};

		WListItemRenderer renderer = new WListItemRenderer(Arrays.asList(columns));
		ListHeader header = new ListHeader();
		ZKUpdateUtil.setWidth(header, "60px");
		renderer.setListHeader(0, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(1, header);
		header = new ListHeader();
		ZKUpdateUtil.setWidth(header, null);
		renderer.setListHeader(2, header);
		renderer.addTableValueChangeListener(listbox);
		model.setNoColumns(columns.length);
		listbox.setModel(model);
		listbox.setItemRenderer(renderer);
		listbox.setSizedByContent(false);
		listbox.repaint();

		return m_activities.length;
	}	//	loadActivities

	/**
	 * 	Reset form and return activity at selIndex
	 *	@param selIndex select index
	 *	@return selected activity
	 */
	private MWFActivity resetDisplay(int selIndex)
	{
		fAnswerText.setVisible(false);
		fAnswerList.setVisible(false);
		fAnswerButton.setVisible(false);
		if (ThemeManager.isUseFontIconForImage())
			fAnswerButton.setIconSclass(Icon.getIconSclass(Icon.WINDOW));
		else
			fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
		fTextMsg.setReadonly(!(selIndex >= 0));
		fTextMsg.setValue("");
		bZoom.setEnabled(selIndex >= 0);
		bOK.setEnabled(selIndex >= 0);
		fForward.setValue(null);
		fForward.setReadWrite(selIndex >= 0);
		//
		statusBar.setStatusDB(String.valueOf(selIndex+1) + "/" + m_activities.length);
		m_activity = null;
		m_column = null;
		if (m_activities.length > 0)
		{
			if (selIndex >= 0 && selIndex < m_activities.length)
				m_activity = m_activities[selIndex];
		}
		//	Nothing to show
		if (m_activity == null)
		{
			fNode.setText ("");
			fDescription.setText ("");
			fHelp.setText ("");
			fHistory.setContent(HISTORY_DIV_START_TAG + "&nbsp;</div>");
			statusBar.setStatusDB("0/" + m_activities.length);
			statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
		}
		return m_activity;
	}	//	resetDisplay

	/**
	 * Display activity at index
	 * @param index
	 */
	public void display (int index)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Index=" + index);
		//
		m_activity = resetDisplay(index);
		//	Nothing to show
		if (m_activity == null)
		{
			return;
		}

		renderTransactionDetails(m_activity);

		//	Display Activity
		fNode.setText (m_activity.getNodeName());
		fDescription.setValue (m_activity.getNodeDescription());
		fHelp.setValue (m_activity.getNodeHelp());
		//
		fHistory.setContent (HISTORY_DIV_START_TAG+m_activity.getHistoryHTML()+"</div>");

		//	User Actions
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
		{
			if (m_column == null)
				m_column = node.getColumn();
			if (m_column != null && m_column.get_ID() != 0)
			{
				fAnswerList.removeAllItems();
				int dt = m_column.getAD_Reference_ID();
				if (dt == DisplayType.YesNo)
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), 319, false);		//	_YesNo
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else if (DisplayType.isList(dt))
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), m_column.getAD_Reference_Value_ID(), false);
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else	//	other display types come here
				{
					fAnswerText.setText ("");
					fAnswerText.setVisible(true);
				}
			}
		}
		//	--
		else if (MWFNode.ACTION_UserWindow.equals(node.getAction())
			|| MWFNode.ACTION_UserForm.equals(node.getAction())
			|| MWFNode.ACTION_UserInfo.equals(node.getAction()))
		{
			fAnswerButton.setLabel(node.getName());
			fAnswerButton.setTooltiptext(node.getDescription());
			fAnswerButton.setVisible(true);
		}
		else
			log.log(Level.SEVERE, "Unknown Node Action: " + node.getAction());

		statusBar.setStatusDB((m_index+1) + "/" + m_activities.length);
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFActivities"));
	}	//	display


	/**
	 * Zoom to workflow activity window
	 */
	private void cmd_zoom()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		AEnv.zoom(m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	}	//	cmd_zoom

	/**
	 * 	Action Button
	 */
	private void cmd_button()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		//
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction()))
		{
			int AD_Window_ID = node.getAD_Window_ID();		// Explicit Window
			String ColumnName = m_activity.getPO().get_TableName() + "_ID";
			int Record_ID = m_activity.getRecord_ID();
			MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
			boolean IsSOTrx = m_activity.isSOTrx();
			//
			if (log.isLoggable(Level.INFO))
				log.info("Zoom to AD_Window_ID=" + AD_Window_ID
					+ " - " + query + " (IsSOTrx=" + IsSOTrx + ")");

			AEnv.zoom(AD_Window_ID, query);
		}
		else if (MWFNode.ACTION_UserForm.equals(node.getAction()))
		{
			int AD_Form_ID = node.getAD_Form_ID();

			ADForm form = ADForm.openForm(AD_Form_ID);
			form.setAttribute(Window.MODE_KEY, form.getWindowMode());
			AEnv.showWindow(form);
		}else if (MWFNode.ACTION_UserInfo.equals(node.getAction())){
			SessionManager.getAppDesktop().openInfo(node.getAD_InfoWindow_ID());
		}
		else
			log.log(Level.SEVERE, "No User Action:" + node.getAction());
	}	//	cmd_button


	/**
	 * 	Save
	 */
	public void onOK()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
		{
			Clients.clearBusy();
			return;
		}
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		String textMsg = fTextMsg.getValue();
		//
		MWFNode node = m_activity.getNode();

		Object forward = fForward.getValue();

		// ensure activity is ran within a transaction - [ 1953628 ]
		Trx trx = null;
		try {
			trx = Trx.get(Trx.createTrxName("FWFA"), true);
			trx.setDisplayName(getClass().getName()+"_onOK");
			m_activity.set_TrxName(trx.getTrxName());

			if (forward != null)
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Forward to " + forward);
				int fw = ((Integer)forward).intValue();
				if (fw == AD_User_ID || fw == 0)
				{
					log.log(Level.SEVERE, "Forward User=" + fw);
					trx.rollback();
					trx.close();
					return;
				}
				if (!m_activity.forwardTo(fw, textMsg))
				{
					Dialog.error(m_WindowNo, "CannotForward");
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Choice - Answer
			else if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
			{
				if (m_column == null)
					m_column = node.getColumn();
				//	Do we have an answer?
				int dt = m_column.getAD_Reference_ID();
				String value = fAnswerText.getText();
				if (dt == DisplayType.YesNo || DisplayType.isList(dt))
				{
					ListItem li = fAnswerList.getSelectedItem();
					if(li != null) value = li.getValue().toString();
				}
				if (value == null || value.length() == 0)
				{
					Dialog.error(m_WindowNo, "FillMandatory", Msg.getMsg(Env.getCtx(), "Answer"));
					trx.rollback();
					trx.close();
					return;
				}
				//
				if (log.isLoggable(Level.CONFIG)) log.config("Answer=" + value + " - " + textMsg);
				try
				{
					m_activity.setUserChoice(AD_User_ID, value, dt, textMsg);
					MWFProcess wfpr = new MWFProcess(m_activity.getCtx(), m_activity.getAD_WF_Process_ID(), m_activity.get_TrxName());
					wfpr.checkCloseActivities(m_activity.get_TrxName());
					
					if (!Util.isEmpty(m_activity.getProcessMsg(), true))
						Dialog.error(m_WindowNo, m_activity.getProcessMsg());
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					Dialog.error(m_WindowNo, "Error", e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage());
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Action
			else
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Action=" + node.getAction() + " - " + textMsg);
				try
				{
					// ensure activity is ran within a transaction
					m_activity.setUserConfirmation(AD_User_ID, textMsg);
					MWFProcess wfpr = new MWFProcess(m_activity.getCtx(), m_activity.getAD_WF_Process_ID(), m_activity.get_TrxName());
					wfpr.checkCloseActivities(m_activity.get_TrxName());
					
					if (!Util.isEmpty(m_activity.getProcessMsg(), true))
						Dialog.error(m_WindowNo, m_activity.getProcessMsg());
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					Dialog.error(m_WindowNo, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}

			}

			trx.commit();
		}
		finally
		{
			Clients.clearBusy();
			if (trx != null)
				trx.close();
		}

		//	Next
		loadActivities();
		display(-1);
	}	//	onOK
}
