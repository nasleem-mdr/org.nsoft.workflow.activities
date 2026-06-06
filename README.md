# Custom Form IDempiere workflow Activities
IDempiere Workflow Approval Form Customize, add detail transaction view and change layout design.

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
 *   WF_DETAIL_C_Order_HDR_TOTAL  
