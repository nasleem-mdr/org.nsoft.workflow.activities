# Custom Form IDempiere workflow Activities
IDempiere Workflow Approval Form Customize, add detail transaction view and change layout design.

Query use System Configuration 
SYSCONFIG KEY PATTERN :
WF_DETAIL_<TableName><Suffix>
───────────────────────────
| Suffix      | Mandatory | Description                 |
|-------------|-----------|-----------------------------|
| _LINE_TABLE | N         | Table line Name             |
| _LINK_COL   | N*        | FK table line, mandatory if |
| _ORDER_BY   | N         | Order by query line         |
| _DESC_COL   | N         | Description                 |
| _QTY_COL    | N         | Quantity Column             |
| _AMT_COL    | N         | Amount Column               |
| _HDR_DOCNO  | N         | Documentno                  |
| _HDR_BP     | N         | Business Partner            |
| _HDR_DATE   | N         | Document Date               |
| _HDR_TOTAL  | N         | Total Amount                |


 * *) LINK_COL wajib jika LINE_TABLE diisi
 *
 * Gunakan "-" sebagai value untuk menonaktifkan kolom tertentu.
 * Jika SysConfig tidak dikonfigurasi sama sekali, renderer tetap
 * tampil dengan nilai fallback generik — tidak crash.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CONTOH KONFIGURASI LENGKAP
 * ─────────────────────────────
C_Order:
WF_DETAIL_C_Order_LINE_TABLE  = C_OrderLine
WF_DETAIL_C_Order_LINK_COL   = C_Order_ID
WF_DETAIL_C_Order_ORDER_BY   = Line,C_Order_ID
WF_DETAIL_C_Order_DESC_COL   = M_Product_ID>Name,Description
WF_DETAIL_C_Order_QTY_COL    = QtyOrdered,QtyEntered
WF_DETAIL_C_Order_AMT_COL    = LineNetAmt
WF_DETAIL_C_Order_HDR_DOCNO  = DocumentNo
WF_DETAIL_C_Order_HDR_BP     = C_BPartner_ID>Name
WF_DETAIL_C_Order_HDR_DATE   = DateOrdered,DateDoc
WF_DETAIL_C_Order_HDR_TOTAL  
