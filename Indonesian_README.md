# WFTransactionDetailRenderer

Dokumentasi teknis untuk class `WFTransactionDetailRenderer.java`  
Plugin: `org.nsoft.workflow.activities` | Versi: 3.1

---

## Daftar Isi

- [Gambaran Umum](#gambaran-umum)
- [Arsitektur](#arsitektur)
- [Cara Kerja](#cara-kerja)
- [Konfigurasi SysConfig](#konfigurasi-sysconfig)
  - [Key Wajib](#key-wajib)
  - [Key Opsional (Ada Default)](#key-opsional-ada-default)
  - [Format Nilai](#format-nilai)
- [Default Fallback](#default-fallback)
- [Tabel yang Didukung](#tabel-yang-didukung)
- [Contoh Konfigurasi Lengkap](#contoh-konfigurasi-lengkap)
- [Logika Resolusi Kolom](#logika-resolusi-kolom)
- [Changelog](#changelog)

---

## Gambaran Umum

`WFTransactionDetailRenderer` adalah class renderer yang menampilkan **detail dokumen transaksi** (header + baris/lines) langsung di dalam panel approval workflow iDempiere (`WWFActivity` form).

Class ini dirancang dengan pendekatan **SysConfig-driven + Generic PO**, sehingga dapat menampilkan detail dari tabel dokumen apapun **tanpa perlu import model class spesifik** — cukup dengan konfigurasi di Application Dictionary (SysConfig).

---

## Arsitektur

```
WWFActivity.java
    └── WFTransactionDetailRenderer.java
            ├── MSysConfig          ← Baca konfigurasi kolom per tabel
            ├── MTable / PO         ← Query header dokumen (generic)
            ├── Query               ← Query baris/lines dokumen
            └── ZK Components       ← Render ke UI (Groupbox, Listbox, Label)
```

**Alur data:**

```
MWFActivity (activity yang dipilih user)
    → ambil AD_Table_ID + Record_ID
    → load PO header via MTable.getPO()
    → baca SysConfig untuk konfigurasi kolom
    → render header (4 label)
    → query & render baris (listbox 3 kolom)
```

---

## Cara Kerja

### 1. Entry Point

Method utama yang dipanggil dari `WWFActivity.display()`:

```java
public void render(MWFActivity activity)
```

Setiap kali user memilih item approval di listbox kiri, method ini dipanggil ulang.

### 2. Render Header

Menampilkan 4 informasi ringkasan dokumen di bagian atas:

| Label UI | Konten Default |
|----------|---------------|
| `lHdrCol1` | No. Dokumen (`DocumentNo`, `Value`, `Name`) |
| `lHdrCol2` | Business Partner (`C_BPartner_ID>Name`) |
| `lHdrCol3` | Tanggal (`DateOrdered`, `DateInvoiced`, dst.) |
| `lHdrCol4` | Total (`GrandTotal`, `TotalLines`) |

### 3. Render Lines

Menampilkan listbox 3 kolom berisi baris/item dokumen:

| Kolom | Konten Default |
|-------|---------------|
| COL1 | Deskripsi produk (`M_Product_ID>Name`) |
| COL2 | Qty (`QtyOrdered`, `QtyInvoiced`, `MovementQty`, dst.) |
| COL3 | Amount (`LineNetAmt`, `PriceActual`) |

---

## Konfigurasi SysConfig

Semua konfigurasi menggunakan prefix: **`WF_DETAIL_<TableName>_<SUFFIX>`**

Contoh untuk tabel `C_Order`: `WF_DETAIL_C_Order_LINE_TABLE`

### Key Wajib

Key berikut **harus dikonfigurasi** agar baris/lines dapat ditampilkan:

| Key | Keterangan | Contoh Nilai |
|-----|------------|--------------|
| `WF_DETAIL_<Table>_LINE_TABLE` | Nama tabel baris/lines | `C_OrderLine` |
| `WF_DETAIL_<Table>_LINK_COL` | Kolom foreign key di tabel baris | `C_Order_ID` |

> **Catatan:** Jika `LINE_TABLE` diisi dengan `-`, maka listbox disembunyikan (dokumen tanpa baris, seperti `C_Payment`).

### Key Opsional (Ada Default)

Key berikut **tidak wajib dikonfigurasi** — jika kosong, sistem menggunakan nilai default (lihat bagian [Default Fallback](#default-fallback)).

#### Header Fields

| Key | Keterangan |
|-----|------------|
| `WF_DETAIL_<Table>_HDR_COL1` | Kolom No. Dokumen |
| `WF_DETAIL_<Table>_HDR_COL1_LABEL` | Label untuk HDR_COL1 |
| `WF_DETAIL_<Table>_HDR_COL2` | Kolom Business Partner |
| `WF_DETAIL_<Table>_HDR_COL2_LABEL` | Label untuk HDR_COL2 |
| `WF_DETAIL_<Table>_HDR_COL3` | Kolom Tanggal |
| `WF_DETAIL_<Table>_HDR_COL3_LABEL` | Label untuk HDR_COL3 |
| `WF_DETAIL_<Table>_HDR_COL4` | Kolom Total/Nilai |
| `WF_DETAIL_<Table>_HDR_COL4_LABEL` | Label untuk HDR_COL4 |
| `WF_DETAIL_<Table>_HDR_COL4_TYPE` | Tipe data HDR_COL4 (`numeric` / `string`) |

#### Line Fields

| Key | Keterangan |
|-----|------------|
| `WF_DETAIL_<Table>_COL1` | Kolom deskripsi baris |
| `WF_DETAIL_<Table>_COL1_LABEL` | Header kolom 1 di listbox |
| `WF_DETAIL_<Table>_COL1_TYPE` | Tipe data (`string` / `numeric`) |
| `WF_DETAIL_<Table>_COL2` | Kolom qty/jumlah |
| `WF_DETAIL_<Table>_COL2_LABEL` | Header kolom 2 di listbox |
| `WF_DETAIL_<Table>_COL2_TYPE` | Tipe data (`string` / `numeric`) |
| `WF_DETAIL_<Table>_COL3` | Kolom amount/nilai |
| `WF_DETAIL_<Table>_COL3_LABEL` | Header kolom 3 di listbox |
| `WF_DETAIL_<Table>_COL3_TYPE` | Tipe data (`string` / `numeric`) |
| `WF_DETAIL_<Table>_ORDER_BY` | Urutan baris (SQL ORDER BY) |

### Format Nilai

#### 1. Comma-Separated Fallback List

Nilai dapat berisi beberapa nama kolom dipisahkan koma. Sistem akan **mencoba satu per satu dari kiri ke kanan** dan berhenti pada nilai pertama yang tidak kosong/null.

```
QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty
```

Cara kerjanya (untuk `C_InvoiceLine`):
```
QtyOrdered  → null (kolom tidak ada) → lanjut
QtyInvoiced → ada nilai! → RETURN ← berhenti di sini
QtyEntered  → tidak sempat dicoba
```

> **Penting:** Ini bukan array — ini adalah **daftar fallback**. Nilai yang pertama ditemukan yang digunakan, bukan semua nilai digabung.

#### 2. FK Chain Lookup

Untuk membaca kolom dari tabel lain via foreign key, gunakan format `>`:

```
C_BPartner_ID>Name
```

Multi-level FK juga didukung:

```
C_BPartner_ID>C_BPartner_Location_ID>C_Location_ID>City
```

#### 3. Nilai `-` (Dash)

Gunakan `-` untuk menonaktifkan fitur secara eksplisit:

- `LINE_TABLE = -` → sembunyikan listbox (dokumen tanpa baris)
- `COL3 = -` → kolom ketiga dikosongkan
- `HDR_COL4 = -` → total tidak ditampilkan

---

## Default Fallback

Nilai default digunakan otomatis jika key SysConfig tidak dikonfigurasi:

### Header

| Key Suffix | Default Value | Keterangan |
|------------|--------------|------------|
| `_HDR_COL1` | `DocumentNo,Value,Name` | No. Dokumen |
| `_HDR_COL1_LABEL` | `No. Dokumen` | |
| `_HDR_COL2` | `C_BPartner_ID>Name` | Business Partner |
| `_HDR_COL2_LABEL` | `Business Partner` | |
| `_HDR_COL3` | `DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created` | Tanggal |
| `_HDR_COL3_LABEL` | `Tanggal` | |
| `_HDR_COL4` | `GrandTotal,TotalLines,-` | Grand Total |
| `_HDR_COL4_LABEL` | `Total` | |
| `_HDR_COL4_TYPE` | `numeric` | |

### Lines

| Key Suffix | Default Value | Keterangan |
|------------|--------------|------------|
| `_COL1` | `M_Product_ID>Name,Description,Name` | Deskripsi |
| `_COL1_LABEL` | `Deskripsi` | |
| `_COL1_TYPE` | `string` | |
| `_COL2` | `QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty` | Qty |
| `_COL2_LABEL` | `Qty` | |
| `_COL2_TYPE` | `numeric` | |
| `_COL3` | `LineNetAmt,PriceActual,-` | Amount |
| `_COL3_LABEL` | `Amount` | |
| `_COL3_TYPE` | `numeric` | |

> Default fallback dirancang cukup generik untuk mencakup sebagian besar tabel dokumen standar iDempiere. Untuk tabel yang memiliki nama kolom unik, disarankan untuk mengkonfigurasi SysConfig secara spesifik.

---

## Tabel yang Didukung

Berikut tabel yang sudah dikonfigurasi via 2Pack XML (`WFDetail_SysConfig.xml`):

| Tabel Header | Tabel Lines | Keterangan |
|--------------|-------------|------------|
| `C_Order` | `C_OrderLine` | Sales/Purchase Order |
| `C_Invoice` | `C_InvoiceLine` | Invoice |
| `M_InOut` | `M_InOutLine` | Shipment / Receipt |
| `M_Requisition` | `M_RequisitionLine` | Purchase Requisition |
| `C_Payment` | `-` (tanpa baris) | Payment |
| `C_BankStatement` | `C_BankStatementLine` | Bank Statement |
| `M_Movement` | `M_MovementLine` | Inventory Movement |
| `M_Inventory` | `M_InventoryLine` | Physical Inventory |
| `HR_Process` | `HR_Line` | Payroll Process |

---

## Contoh Konfigurasi Lengkap

### C_Order (Minimal — hanya key wajib)

```
WF_DETAIL_C_Order_LINE_TABLE  = C_OrderLine
WF_DETAIL_C_Order_LINK_COL    = C_Order_ID
```

Kolom lainnya akan menggunakan default fallback secara otomatis.

### C_Invoice (Custom — COL2 pakai QtyEntered duluan)

Default fallback untuk COL2 adalah `QtyOrdered,QtyInvoiced,...`, sehingga `QtyInvoiced` yang akan dipakai duluan untuk `C_InvoiceLine`. Jika ingin menggunakan `QtyEntered`:

```
WF_DETAIL_C_Invoice_LINE_TABLE  = C_InvoiceLine
WF_DETAIL_C_Invoice_LINK_COL    = C_Invoice_ID
WF_DETAIL_C_Invoice_COL2        = QtyEntered,QtyInvoiced
```

### C_Payment (Tanpa Baris)

```
WF_DETAIL_C_Payment_LINE_TABLE  = -
WF_DETAIL_C_Payment_HDR_COL4    = PayAmt
WF_DETAIL_C_Payment_HDR_COL4_LABEL = Jumlah Pembayaran
```

### HR_Process (Custom Label Bahasa Indonesia)

```
WF_DETAIL_HR_Process_LINE_TABLE   = HR_Line
WF_DETAIL_HR_Process_LINK_COL     = HR_Process_ID
WF_DETAIL_HR_Process_COL1         = HR_Concept_ID>Name
WF_DETAIL_HR_Process_COL1_LABEL   = Konsep Gaji
WF_DETAIL_HR_Process_COL2         = Qty
WF_DETAIL_HR_Process_COL2_LABEL   = Jumlah
WF_DETAIL_HR_Process_COL3         = Amount
WF_DETAIL_HR_Process_COL3_LABEL   = Nilai
```

---

## Logika Resolusi Kolom

### `resolveColumnValue()` — Fallback List

```
Input  : "QtyOrdered,QtyInvoiced,MovementQty"
Proses : coba QtyOrdered → null? → coba QtyInvoiced → ada nilai → return
Output : nilai QtyInvoiced
```

Mirip **null coalescing operator** (`??`) tapi untuk nama kolom di PO.

### `resolveOneColumn()` — FK Chain

```
Input  : "C_BPartner_ID>Name"
Proses : ambil nilai C_BPartner_ID dari PO saat ini
       → load PO C_BPartner dengan ID tersebut
       → ambil kolom Name
Output : nama business partner
```

### `splitColumnDefs()` — Pemisahan Cerdas

Pemisahan koma memperhatikan konteks FK chain:

```
Input  : "M_Product_ID>Name,Description"
Split  : ["M_Product_ID>Name", "Description"]
         ↑ FK chain tetap utuh  ↑ fallback terpisah
```

Tanda `,` setelah `>` dianggap sebagai bagian dari FK chain, bukan pemisah kandidat.

### `formatByType()` — Format Berdasarkan Tipe

| Tipe | Format Output |
|------|---------------|
| `numeric` | Number format dengan pemisah ribuan (contoh: `1.500.000`) |
| `string` | Plain text, tidak diformat |

---

## Changelog

### v3.1 — Bug Fixes
- **Fix:** Guard `"-".equals(lineTable)` dipindahkan sebelum pengecekan `LINK_COL` di `validateAndWarnConfig()` — sebelumnya memunculkan warning yang tidak perlu ketika `LINE_TABLE="-"` (sengaja dinonaktifkan).
- **Fix:** `resolveColumnValue()` menggunakan `splitColumnDefs()` — sebelumnya split langsung dengan `","` menyebabkan FK chain seperti `C_BPartner_ID>Name,Description` terpecah secara salah.
- **Fix:** `renderListhead()` menggunakan `lstTxLines.appendChild(head)` — sebelumnya menggunakan `insertBefore()` yang tidak kompatibel dengan versi ZK yang terinstal.

### v3.0
- Penambahan `COL1/COL2/COL3` dengan label dinamis dan type-aware formatting.
- Multi-level FK lookup.
- Flexible column header.

---

*Dokumentasi ini dibuat untuk plugin `org.nsoft.workflow.activities` — iDempiere ERP customization.*
