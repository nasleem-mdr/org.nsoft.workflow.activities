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
package org.nsoft.webui.apps.wf;

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
//import org.adempiere.webui.component.ListItem;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listbox; 
import org.adempiere.webui.component.ListModelTable;
//import org.adempiere.webui.component.Listbox;
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
import org.compiere.model.MTable;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MRefList;
import org.compiere.model.SystemIDs;
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
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.South;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabpanel;
import org.compiere.model.Query;
import org.compiere.model.MSysConfig;
import org.compiere.model.MQuery;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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

	// Components
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
	private Label lHdrCol1      = new Label();
    private Label lHdrCol2      = new Label();
    private Label lHdrCol3      = new Label();
    private Label lHdrCol4      = new Label();
	private Label lHdrCol1Title = new Label("Document No");
    private Label lHdrCol2Title = new Label("Business Partner");
    private Label lHdrCol3Title = new Label("Date");
    private Label lHdrCol4Title = new Label("Total");
	private Tabbox tabboxDetail = new Tabbox();
	private Tabpanels tabpanels = new Tabpanels();
	private WFTransactionDetailRenderer txRenderer;
	private West westPanel = new West();
	private org.zkoss.zul.Listbox cardListbox = new org.zkoss.zul.Listbox();
	private Tab tabDetail = new Tab("Detail");
	
	private Hlayout createModernActionButtons() {
       Hlayout buttonLayout = new Hlayout();
       buttonLayout.setHflex("1");
       buttonLayout.setSpacing("10px");

       Button btnApprove = new Button("Approve");
       btnApprove.setHeight("38px");
       btnApprove.setHflex("1");
       btnApprove.setSclass("wf-btn-approve");  
       btnApprove.addEventListener(Events.ON_CLICK, e -> executeApprovalDirectly(true));

       Button btnReject = new Button("Reject");
       btnReject.setHeight("38px");
       btnReject.setHflex("1");
       btnReject.setSclass("wf-btn-reject");
       btnReject.addEventListener(Events.ON_CLICK, e -> executeApprovalDirectly(false));

       buttonLayout.appendChild(btnReject);
       buttonLayout.appendChild(btnApprove);

       return buttonLayout;
    }

	public WWFActivity()
	{
		super();
	    LayoutUtils.addSclass("workflow-activity-form", this);
	}

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
		txRenderer = new WFTransactionDetailRenderer(
                grpTxDetails,   lstTxLines,
                lHdrCol1,       lHdrCol1Title,
                lHdrCol2,       lHdrCol2Title,
                lHdrCol3,       lHdrCol3Title,
                lHdrCol4,       lHdrCol4Title
        );
		
		display(-1);
       
    }

	private void setTooltipText(Button btn, String key) {
		String text = Util.cleanAmp(Msg.translate(Env.getCtx(), key));
		if (!Util.isEmpty(text, true))
			btn.setTooltiptext(text);
	}

	private void init()
	{
		//inject css style
        injectBundleStyle("web/css/wf-style.css");
        // Part 1: West Panel (Approval List)
	    westPanel = new West();
	    westPanel.setSize("320px");
	    westPanel.setCollapsible(true);
		westPanel.setSplittable(true);
		westPanel.setOpen(true);
	    westPanel.setTitle("Approval List");
	    westPanel.setSclass("wf-west-panel"); 
		
	    cardListbox.setSclass("wf-approval-listbox");
	    cardListbox.setVflex("1");
	    cardListbox.setHflex("1");
	    westPanel.appendChild(cardListbox);
	    cardListbox.addEventListener(Events.ON_SELECT, this);
	
	    // Part 2: Center Panel - Approval Node
	    Vlayout nodeApprovalArea = new Vlayout();
	    nodeApprovalArea.setHflex("1");
	    nodeApprovalArea.setSpacing("8px");
	    nodeApprovalArea.setSclass("wf-card-container");

	    Hlayout nodeHeader = new Hlayout();
	    nodeHeader.setHflex("1");
	    titleNode.setSclass("wf-section-title");
	    nodeHeader.appendChild(titleNode);
		nodeApprovalArea.appendChild(nodeHeader);
	    
	    Vlayout nodeGroup = new Vlayout();
	    nodeGroup.setSpacing("3px");
	    lNode.setSclass("wf-field-label");
	    nodeGroup.appendChild(lNode);
	    nodeGroup.appendChild(fNode);
	    ZKUpdateUtil.setHflex(fNode, "true");
	    fNode.setReadonly(true);
	    fNode.setSclass("wf-field-input-readonly");
	    nodeApprovalArea.appendChild(nodeGroup);

	    Vlayout descGroup = new Vlayout();
	    descGroup.setSpacing("3px");
	    lDesctiption.setSclass("wf-field-label");
	    descGroup.appendChild(lDesctiption);
	    descGroup.appendChild(fDescription);
	    ZKUpdateUtil.setHflex(fDescription, "true");
	    fDescription.setMultiline(true);
	    fDescription.setReadonly(true);
	    fDescription.setSclass("wf-field-input-readonly wf-min-height-40");
	    nodeApprovalArea.appendChild(descGroup);
	
	    // Part 3.1: Structure Main Tabbox
	    
	    tabboxDetail.setSclass("wf-tabbox-custom"); 
	    Tabs tabs = new Tabs();
	    tabs.appendChild(tabDetail);   
	    tabs.appendChild(new Tab(Msg.translate(Env.getCtx(), "History")));
	    tabs.appendChild(new Tab(Msg.translate(Env.getCtx(), "Help")));
	    tabboxDetail.appendChild(tabs);       
	    tabboxDetail.appendChild(tabpanels);  
	
	    // Part 3.2: Tabpanel 1 - Document Details
	    Tabpanel panelLines = new Tabpanel();
	    tabpanels.appendChild(panelLines);
	        
	    grpTxDetails.setOpen(true);
	    grpTxDetails.setHflex("1");
	    grpTxDetails.setVisible(true); 
	    grpTxDetails.setSclass("wf-groupbox-clean"); 
	
	    		
		Vlayout headerInfoLayout = new Vlayout();
		headerInfoLayout.setHflex("1");
		headerInfoLayout.setSpacing("4px");
		headerInfoLayout.setSclass("wf-header-info");

		// BARIS ATAS (Kolom 1 dan Kolom 3)
		Hlayout topRow = new Hlayout();
		topRow.setHflex("1");
		topRow.setSpacing("10px"); // Jarak antara kolom kiri dan kanan

		    // Kolom kiri (No. Dokumen)
		    Hlayout col1 = new Hlayout();
		    col1.setHflex("1");
		    col1.setSpacing("0px");
		    lHdrCol1Title.setSclass("wf-field-label wf-label-min");
		    lHdrCol1.setSclass("wf-field-value");
		    Label sep1 = new Label(":");
		    sep1.setSclass("wf-label-sep");
		    col1.appendChild(lHdrCol1Title);
		    col1.appendChild(sep1);
		    col1.appendChild(lHdrCol1);
		    topRow.appendChild(col1);

		    // Kolom kanan (Date)
		    Hlayout col3 = new Hlayout();
		    col3.setHflex("1");
		    col3.setSpacing("0px");
		    lHdrCol3Title.setSclass("wf-field-label wf-label-min");
		    lHdrCol3.setSclass("wf-field-value");
		    Label sep3 = new Label(":");
		    sep3.setSclass("wf-label-sep");
		    col3.appendChild(lHdrCol3Title);
		    col3.appendChild(sep3);
		    col3.appendChild(lHdrCol3);
		    topRow.appendChild(col3);

		headerInfoLayout.appendChild(topRow);
		// BARIS BAWAH (Kolom 2 network dan Kolom 4)
		Hlayout bottomRow = new Hlayout();
		bottomRow.setHflex("1");
		bottomRow.setSpacing("10px"); // Jarak antara kolom kiri dan kanan

		    // Kolom kiri (Business Partner)
		    Hlayout col2 = new Hlayout();
		    col2.setHflex("1");
		    col2.setSpacing("0px");
		    lHdrCol2Title.setSclass("wf-field-label wf-label-min");
		    lHdrCol2.setSclass("wf-field-value");
		    Label sep2 = new Label(":");
		    sep2.setSclass("wf-label-sep");
		    col2.appendChild(lHdrCol2Title);
		    col2.appendChild(sep2);
		    col2.appendChild(lHdrCol2);
		    bottomRow.appendChild(col2);

		    // Kolom kanan (Total)
		    Hlayout col4 = new Hlayout();
		    col4.setHflex("1");
		    col4.setSpacing("0px");
		    lHdrCol4Title.setSclass("wf-field-label wf-label-min");
		    lHdrCol4.setSclass("wf-field-value wf-value-numeric");
		    Label sep4 = new Label(":");
		    sep4.setSclass("wf-label-sep");
		    col4.appendChild(lHdrCol4Title);
		    col4.appendChild(sep4);
		    col4.appendChild(lHdrCol4);
		    bottomRow.appendChild(col4);

		headerInfoLayout.appendChild(bottomRow);
	    grpTxDetails.appendChild(headerInfoLayout);
	    
		
	    lstTxLines.setHflex("1");
	    lstTxLines.setSpan(true);
	    lstTxLines.setSclass("mobile-scrollable-list wf-details-listbox");
	
	    grpTxDetails.appendChild(lstTxLines); 
	    panelLines.appendChild(grpTxDetails); 
	    
	    // Part 3.3: Create Tabpanel 2 - History
	    Tabpanel panelHistory = new Tabpanel(); 
	    tabpanels.appendChild(panelHistory);  
	    
	    Vlayout historyLayout = new Vlayout();
	    historyLayout.setHflex("1");
	    historyLayout.setSpacing("5px");
	    lHistory.setSclass("wf-section-title-sm");
	    historyLayout.appendChild(lHistory);
	    historyLayout.appendChild(fHistory);
	    fHistory.setSclass("wf-history-textarea");
	    panelHistory.appendChild(historyLayout);
	 // Part 3.4: Tabpanel 3 - Help — BARU
	    Tabpanel panelHelp = new Tabpanel();
	    tabpanels.appendChild(panelHelp);

	    Vlayout helpTabLayout = new Vlayout();
	    helpTabLayout.setHflex("1");
	    helpTabLayout.setSpacing("5px");
	    helpTabLayout.appendChild(fHelp);           
	    fHelp.setMultiline(true);
	    fHelp.setRows(5);
	    fHelp.setReadonly(true);
	    fHelp.setHflex("true");
	    fHelp.setSclass("wf-field-input-readonly wf-help-text");
	    panelHelp.appendChild(helpTabLayout);
	    
	    // Part 4: Center Panel - Approval Action (Footer)
	    Vlayout footerApprovalArea = new Vlayout();
	    footerApprovalArea.setHflex("1");
	    footerApprovalArea.setSpacing("12px");
	    footerApprovalArea.setSclass("wf-card-container wf-margin-top-10");
	
	    titleAction.setSclass("wf-section-title");
	    footerApprovalArea.appendChild(titleAction);
	
	    Vlayout msgGroup = new Vlayout();
	    msgGroup.setSpacing("5px");
	    lTextMsg.setSclass("wf-field-label");
	    msgGroup.appendChild(lTextMsg);
	    msgGroup.appendChild(fTextMsg);
	    fTextMsg.setMultiline(true);
	    fTextMsg.setRows(3);
	    ZKUpdateUtil.setWidth(fTextMsg, "100%");
	    fTextMsg.setSclass("wf-textarea-action");
	    footerApprovalArea.appendChild(msgGroup);
	    
	    fTextMsg.setSclass("wf-textarea-action");
	    footerApprovalArea.appendChild(msgGroup);

	    // Sembunyikan answer components — digantikan Approve/Reject
	    fAnswerText.setVisible(false);
	    fAnswerList.setVisible(false);
	    fAnswerButton.setVisible(false);
	    fAnswerButton.addEventListener(Events.ON_CLICK, this);
	    bZoom.addEventListener(Events.ON_CLICK, this);

	    // Forward section — bZoom dipindah ke sebelah bOK bRefresh
	    Vlayout forwardSection = new Vlayout();
	    forwardSection.setSpacing("5px");
	    lForward.setSclass("wf-field-label");
	    forwardSection.appendChild(lForward);

	    Hlayout forwardActions = new Hlayout();
	    forwardActions.setHflex("1");
	    forwardActions.setValign("middle");
	    forwardActions.appendChild(fForward.getComponent());
	    forwardActions.appendChild(bZoom);
	    forwardActions.appendChild(bRefresh);
	    bOK.addEventListener(Events.ON_CLICK, this); 
	    bRefresh.addEventListener(Events.ON_CLICK, this);
	    forwardSection.appendChild(forwardActions);
	    footerApprovalArea.appendChild(forwardSection);
	       	
	    // Part 5: Main layout
	    Borderlayout mainChatLayout = new Borderlayout();
	    ZKUpdateUtil.setWidth(mainChatLayout, "100%");
	    ZKUpdateUtil.setHeight(mainChatLayout, "100%");
	    mainChatLayout.setSclass("wf-main-layout");
	
	    mainChatLayout.appendChild(westPanel);
	
	    Center centerPanel = new Center();
	    centerPanel.setSclass("wf-center-panel");
		centerPanel.setAutoscroll(true);
	
	    Vlayout chatAreaLayout = new Vlayout();
	    chatAreaLayout.setHflex("1");
	    chatAreaLayout.setSpacing("15px");
	    chatAreaLayout.setSclass("wf-chat-area-padding");
	
	    chatAreaLayout.appendChild(nodeApprovalArea);   
	    chatAreaLayout.appendChild(tabboxDetail);       
	    chatAreaLayout.appendChild(footerApprovalArea);  
	
	    centerPanel.appendChild(chatAreaLayout);
	    mainChatLayout.appendChild(centerPanel);  
	    
		South south = new South();
		south.setSclass("wf-south-panel");
		
		// Buat layout untuk south: statusbar kiri, tombol kanan
		Hlayout southLayout = new Hlayout(); 
		southLayout.setHflex("1");
        southLayout.setSclass("wf-south-layout");
		southLayout.appendChild(statusBar);
		
		Hlayout actionButtons = createModernActionButtons();
		southLayout.appendChild(actionButtons);
		
		south.appendChild(southLayout);
		mainChatLayout.appendChild(south);
	
	    this.appendChild(mainChatLayout);
	    this.setSclass("wf-window-root");
	}
	
	private void injectBundleStyle(String resourcePath) {
	    Bundle bundle = FrameworkUtil.getBundle(WWFActivity.class);
	    URL url = bundle.getResource(resourcePath);
	    if (url == null) {
	        log.warning("Bundle resource not found: " + resourcePath);
	        return;
	    }
	    try (InputStream is = url.openStream();
	         Scanner sc = new Scanner(is, StandardCharsets.UTF_8)) {
	        String css = sc.useDelimiter("\\A").next();
	        // Manual escape
	        String escaped = css
	            .replace("\\", "\\\\")
	            .replace("'", "\\'")
	            .replace("\r\n", "\\n")
	            .replace("\n", "\\n")
	            .replace("\r", "\\n");
	        Clients.evalJavaScript(
	            "(function(c){var s=document.createElement('style');" +
	            "s.textContent=c;document.head.appendChild(s);})('"+escaped+"');"
	        );
	    } catch (IOException e) {
	        log.log(java.util.logging.Level.WARNING, "Failed to inject style: " + resourcePath, e);
	    }
	}
	private String getTableDisplayName(MWFActivity activity) {
	    if (activity == null) return "Detail";
	    try {
	        MTable table = MTable.get(Env.getCtx(), activity.getAD_Table_ID());
	        if (table == null) return "Detail";
	        // Ambil nama table dari AD_Table — sudah di-translate sesuai language
	        String name = table.get_Translation("Name");
	        if (name == null || name.trim().isEmpty())
	            name = table.getName();
	        return name != null ? name : "Detail";
	    } catch (Exception e) {
	        return "Detail";
	    }
	}
	//change two user action[choose yes/no then clik bOK] to one step [choose Approve/Reject] 
	private void executeApprovalDirectly(boolean isApproved) {
	    if (m_activity == null) return;

	    try {
	        // Check forward field
	        Object forward = fForward.getValue();
	        
	        if (forward != null) {
	            // If forward not null, trigger bOK to process forward
	            Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
	            Events.echoEvent("onOK", this, null);
	            return;
	        }

	        // If forward null — process Approve/Reject normally
	        if (fAnswerList.isVisible() && fAnswerList.getItemCount() > 0) {
	            String targetValue = isApproved ? "Y" : "N";
	            boolean itemFound = false;
	            
	            for (int i = 0; i < fAnswerList.getItemCount(); i++) {
	                Listitem li = fAnswerList.getItemAtIndex(i);
	                if (targetValue.equalsIgnoreCase(li.getValue())) {
	                    fAnswerList.setSelectedIndex(i);
	                    itemFound = true;
	                    break;
	                }
	            }
	            
	            if (!itemFound) {
	                fAnswerList.setSelectedIndex(isApproved ? 0 : 1);
	            }
	            
	        } else if (fAnswerText.isVisible()) {
	            fAnswerText.setText(isApproved ? "Approved" : "Rejected");
	        }

	        Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
	        Events.echoEvent("onOK", this, null);
	        
	    } catch (Exception ex) {
	        log.log(java.util.logging.Level.SEVERE, "Failed to execute approval", ex);
	    }
	}
	// Helper for flexible header ratio (hflex)
    private org.zkoss.zul.Listheader createHeader(String label, String ratio) {
        Listheader header = new Listheader(label);
        header.setHflex(ratio); 
        return header;
    }

	private void collapseWestPanel() {
       westPanel.setOpen(false);
    }

    private void expandWestPanel() {
       westPanel.setOpen(true);
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
	        else if (Events.ON_SELECT.equals(eventName) && comp == cardListbox)
	        {
	            m_index = cardListbox.getSelectedIndex();
	            if (m_index >= 0) {
	                display(m_index);
	                collapseWestPanel();
	            }
	        }
			else if ("onOK".equals(eventName))  
	        {
	            onOK();
	    	}
	        else
	        {
	    		super.onEvent(event);
	        }
	}

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
	        list.add(activity);
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
	    // Assign m_activities yang hilang!
	    m_activities = new MWFActivity[list.size()];
	    list.toArray(m_activities);

	    if (log.isLoggable(Level.FINE)) log.fine("#" + m_activities.length + "(" + (System.currentTimeMillis()-start) + "ms)");
	    m_index = 0;

	    // Bersihkan dan render ulang
	    cardListbox.getChildren().clear();
	    model.setNoColumns(1);
	    cardListbox.setModel(model);
	    cardListbox.setItemRenderer(new WFCardRenderer());
	    cardListbox.setSizedByContent(false);

	    return m_activities.length;
	}
	
	private class WFCardRenderer implements org.zkoss.zul.ListitemRenderer<List<Object>> {
	    @Override
	    public void render(org.zkoss.zul.Listitem item, List<Object> data, int index) throws Exception {
	        MWFActivity act = m_activities[index];
	        
	        int priorityVal = act.getPriority();
	        String priority = String.valueOf(priorityVal);
	        String nodeName = act.getNodeName() != null ? act.getNodeName() : "";
	        String summary = act.getSummary() != null ? act.getSummary() : "";

		     // Summary format: "Approve Sales Order Order 80017: GardenUser Standard"
		     // Pisahkan di titik dua (:) untuk dapat DocNo dan UserName
		     String summaryLine1 = summary;
		     String summaryLine2 = "";
	
		     int colonIdx = summary.indexOf(":");
		     if (colonIdx > 0) {
		         summaryLine1 = summary.substring(0, colonIdx + 1).trim(); 
		         summaryLine2 = summary.substring(colonIdx + 1).trim();    
		     }

	        // Dynamic Badge Color based on priority
	        String badgeClass;
	        if (priorityVal <= 3) {
	            badgeClass = "wf-priority-green";
	        } else if (priorityVal <= 6) {
	            badgeClass = "wf-priority-yellow";
	        } else {
	            badgeClass = "wf-priority-red";
	        }
	        
	        String html =
	            "<div class='wf-card-item'>" +
	            "  <div class='wf-card-priority'>" +
	            "    <div>Priority :</div>" +
	            "    <div><span class='wf-card-priority-value " + badgeClass + "'>" + priority + "</span></div>" +
	            "  </div>" +
	            "  <div class='wf-card-divider'></div>" +
	            "  <div class='wf-card-node'>" + nodeName + "</div>" +
	            "  <div class='wf-card-divider'></div>" +
	            "  <div class='wf-card-summary'>" + summaryLine1 + "</div>" +
	            (!summaryLine2.isEmpty() ? "<div class='wf-card-summary-sub'>" + summaryLine2 + "</div>" : "") +
	            "</div>";

	        org.zkoss.zul.Listcell cell = new org.zkoss.zul.Listcell();
	        org.zkoss.zul.Html htmlComp = new org.zkoss.zul.Html();
	        htmlComp.setContent(html);
	        cell.appendChild(htmlComp);
	        item.appendChild(cell);
	        item.setSclass("wf-card-listitem");
	    }
	}
	private String buildTimelineHTML(String rawHtml) {
	    if (rawHtml == null || rawHtml.isEmpty()) 
	        return "";

	    // Parse setiap <p> entry
	    java.util.regex.Pattern pPattern = java.util.regex.Pattern.compile(
	        "<p>(.*?)</p>", 
	        java.util.regex.Pattern.DOTALL
	    );
	    java.util.regex.Matcher pMatcher = pPattern.matcher(rawHtml);

	    // Format tanggal dari history
	    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
	        "MMM dd, yyyy, h:mm:ss a z", java.util.Locale.ENGLISH
	    );

	    java.util.List<java.util.Date> dates = new java.util.ArrayList<>();
	    java.util.List<String> entries = new java.util.ArrayList<>();

	    while (pMatcher.find()) {
	        String entry = pMatcher.group(1).trim();
	        if (entry.isEmpty()) continue;
	        entries.add(entry);

	        // Extract tanggal dari awal string (sebelum <b>)
	        try {
	            // Ambil text sebelum <b>
	            String textOnly = entry.replaceAll("<[^>]+>", "").trim();
	            // Format: "May 22, 2026, 3:37:33 PM WIB (Start):"
	            // Ambil bagian tanggal saja: "May 22, 2026, 3:37:33 PM WIB"
	            java.util.regex.Matcher dateMatcher = java.util.regex.Pattern
	                .compile("^(\\w+ \\d+, \\d+, \\d+:\\d+:\\d+ (?:AM|PM) \\w+)")
	                .matcher(textOnly);
	            if (dateMatcher.find()) {
	                dates.add(sdf.parse(dateMatcher.group(1)));
	            } else {
	                dates.add(null);
	            }
	        } catch (Exception e) {
	            dates.add(null);
	        }
	    }

	    if (entries.isEmpty()) return "";

	    StringBuilder sb = new StringBuilder();
	    sb.append("<div class='wf-timeline'>");

	    for (int i = 0; i < entries.size(); i++) {
	        String entry = entries.get(i);

	        // Extract bagian-bagian entry
	        // Tanggal: sebelum <b>
	        String dateStr = "";
	        java.util.regex.Matcher dateTxtMatcher = java.util.regex.Pattern
	            .compile("^([^<]+)")
	            .matcher(entry);
	        if (dateTxtMatcher.find()) {
	            dateStr = dateTxtMatcher.group(1).trim();
	            if (dateStr.endsWith(":")) 
	                dateStr = dateStr.substring(0, dateStr.length()-1).trim();
	        }

	        // Action: isi <b>
	        String action = "";
	        java.util.regex.Matcher bMatcher = java.util.regex.Pattern
	            .compile("<b>(.*?)</b>")
	            .matcher(entry);
	        if (bMatcher.find()) 
	            action = bMatcher.group(1).replaceAll("[()]", "").trim();

	        // Detail: isi <i>
	        String detail = "";
	        java.util.regex.Matcher iMatcher = java.util.regex.Pattern
	            .compile("<i>(.*?)</i>")
	            .matcher(entry);
	        if (iMatcher.find()) 
	            detail = iMatcher.group(1).trim();

	        // Hitung selisih hari dari entry sebelumnya
	        String dotClass = "wf-tl-dot-green"; // default
	        String daysBadge = "";

	        if (i > 0 && dates.get(i) != null && dates.get(i-1) != null) {
	            long diffMs = dates.get(i).getTime() - dates.get(i-1).getTime();
	            long diffDays = diffMs / (1000 * 60 * 60 * 24);

	            if (diffDays <= 1) {
	                dotClass = "wf-tl-dot-green";
	                daysBadge = diffDays + "d";
	            } else if (diffDays <= 4) {
	                dotClass = "wf-tl-dot-yellow";
	                daysBadge = diffDays + "d";
	            } else {
	                dotClass = "wf-tl-dot-red";
	                daysBadge = diffDays + "d";
	            }
	        }

	        boolean isLast = (i == entries.size() - 1);

	        sb.append("<div class='wf-tl-entry'>")
	          .append("  <div class='wf-tl-left'>")
	          .append("    <div class='wf-tl-dot ").append(dotClass).append("'></div>")
	          .append(isLast ? "" : "<div class='wf-tl-line'></div>")
	          .append("  </div>")
	          .append("  <div class='wf-tl-content'>")
	          .append("    <div class='wf-tl-date'>").append(dateStr);

	        if (!daysBadge.isEmpty()) {
	            sb.append(" <span class='wf-tl-badge ").append(dotClass).append("'>+").append(daysBadge).append("</span>");
	        }

	        sb.append("    </div>")
	          .append("    <div class='wf-tl-action'>").append(action).append("</div>");

	        if (!detail.isEmpty()) {
	            sb.append("<div class='wf-tl-detail'>").append(detail).append("</div>");
	        }

	        sb.append("  </div>")
	          .append("</div>");
	    }

	    sb.append("</div>");
	    return sb.toString();
	}
	private MWFActivity resetDisplay(int selIndex)
	{
		fAnswerText.setVisible(false);
		fAnswerList.setVisible(false);
		fAnswerButton.setVisible(false);
		tabDetail.setLabel("Detail");
		if (ThemeManager.isUseFontIconForImage())
			fAnswerButton.setIconSclass(Icon.getIconSclass(Icon.WINDOW));
		else
			fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
		fTextMsg.setReadonly(!(selIndex >= 0));
		fTextMsg.setValue("");
		bZoom.setEnabled(selIndex >= 0);
		// bOK.setEnabled(selIndex >= 0); // disabled — bOK hidden, replaced by Approve/Reject buttons
		fForward.setValue(null);
		fForward.setReadWrite(selIndex >= 0);
		
		statusBar.setStatusDB(String.valueOf(selIndex+1) + "/" + m_activities.length);
		m_activity = null;
		m_column = null;
		if (m_activities.length > 0)
		{
			if (selIndex >= 0 && selIndex < m_activities.length)
				m_activity = m_activities[selIndex];
		}
		if (m_activity == null)
		{
		    fNode.setText("");
		    fDescription.setText("");
		    fHelp.setText("");
		    fHistory.setContent("<div class='wf-timeline'></div>");
		    statusBar.setStatusDB("0/" + m_activities.length);
		    statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
		}
		return m_activity;
	}	

	public void display (int index)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Index=" + index);
		m_activity = resetDisplay(index);
		if (m_activity == null)
		{
			txRenderer.render(null);
			return;
		}

		txRenderer.render(m_activity);
		
		tabDetail.setLabel(getTableDisplayName(m_activity) + " Detail");
		fNode.setText (m_activity.getNodeName());
		fDescription.setValue (m_activity.getNodeDescription());
		fHelp.setValue (m_activity.getNodeHelp());
		String historyHtml = m_activity.getHistoryHTML();
		fHistory.setContent(
		    historyHtml != null && !historyHtml.isEmpty() 
		        ? buildTimelineHTML(historyHtml) 
		        : "<div class='wf-timeline'></div>"
		);

		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
		{
			if (m_column == null)
				m_column = node.getColumn();
			if (m_column != null && m_column.get_ID() != 0)
			{
				fAnswerList.getItems().clear();
				int dt = m_column.getAD_Reference_ID();
				if (dt == DisplayType.YesNo)
				{
				    ValueNamePair[] values = MRefList.getList(Env.getCtx(), 319, false);
				    for(int i = 0; i < values.length; i++)
				    {
				        Listitem item = new Listitem(values[i].getName());
				        item.setValue(values[i].getValue());
				        fAnswerList.appendChild(item);
				    }
				    fAnswerList.setVisible(true);
				}
				else if (DisplayType.isList(dt))
				{
				    ValueNamePair[] values = MRefList.getList(Env.getCtx(), m_column.getAD_Reference_Value_ID(), false);
				    for(int i = 0; i < values.length; i++)
				    {
				        Listitem item = new Listitem(values[i].getName());
				        item.setValue(values[i].getValue());
				        fAnswerList.appendChild(item);
				    }
				    fAnswerList.setVisible(true);
				}
				else	
				{
					fAnswerText.setText ("");
					fAnswerText.setVisible(true);
				}
			}
		}
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
	}	

	private void cmd_zoom()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		AEnv.zoom(m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	}	

	private void cmd_button()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction()))
		{
			int AD_Window_ID = node.getAD_Window_ID();		
			String ColumnName = m_activity.getPO().get_TableName() + "_ID";
			int Record_ID = m_activity.getRecord_ID();
			//MQuery query = new MQuery(ColumnName, "=", Record_ID);
			MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
			boolean IsSOTrx = m_activity.isSOTrx();
			
			if (log.isLoggable(Level.INFO))
				log.info("Zoom to AD_Window_ID=" + AD_Window_ID + " - " + query + " (IsSOTrx=" + IsSOTrx + ")");

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
	}	

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
		MWFNode node = m_activity.getNode();
		Object forward = fForward.getValue();

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
			else if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
			{
				if (m_column == null)
					m_column = node.getColumn();
				
				int dt = m_column.getAD_Reference_ID();
				String value = fAnswerText.getText();
				if (dt == DisplayType.YesNo || DisplayType.isList(dt))
				{
					//ListItem li = fAnswerList.getSelectedItem();
					//if(li != null) value = li.getValue().toString();
					Listitem li = fAnswerList.getSelectedItem();
					if (li != null) value = (String) li.getValue();
					
				}
				if (value == null || value.length() == 0)
				{
					Dialog.error(m_WindowNo, "FillMandatory", Msg.getMsg(Env.getCtx(), "Answer"));
					trx.rollback();
					trx.close();
					return;
				}
				
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
			else
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Action=" + node.getAction() + " - " + textMsg);
				try
				{
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

		loadActivities();
		display(-1);
	}	
}
