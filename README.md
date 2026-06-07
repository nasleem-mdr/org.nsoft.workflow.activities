# Custom Workflow Activities 

Technical documentation for plugin org.nsoft.workflow.activities especially `WFTransactionDetailRenderer.java`  
Plugin: `org.nsoft.workflow.activities` | Version: 1.3.1

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [How It Works](#how-it-works)
- [SysConfig Configuration](#sysconfig-configuration)
  - [Required Keys](#required-keys)
  - [Optional Keys (With Defaults)](#optional-keys-with-defaults)
  - [Value Format](#value-format)
- [Default Fallback Values](#default-fallback-values)
- [Supported Tables](#supported-tables)
- [Configuration Examples](#configuration-examples)
- [Column Resolution Logic](#column-resolution-logic)
- [Changelog](#changelog)

---

## Overview

`WFTransactionDetailRenderer` is a renderer class that displays **transaction document details** (header + line items) directly inside the iDempiere workflow approval panel (`WWFActivity` form).

This class is designed using a **SysConfig-driven + Generic PO** approach, enabling it to display details from any document table **without importing specific model classes** — configuration in the Application Dictionary (SysConfig) is all that is needed.

---

## Architecture

```
WWFActivity.java
    └── WFTransactionDetailRenderer.java
            ├── MSysConfig          ← Read column configuration per table
            ├── MTable / PO         ← Query document header (generic)
            ├── Query               ← Query document lines
            └── ZK Components       ← Render to UI (Groupbox, Listbox, Label)
```

**Data flow:**

```
MWFActivity (activity selected by user)
    → get AD_Table_ID + Record_ID
    → load header PO via MTable.getPO()
    → read SysConfig for column configuration
    → render header (4 labels)
    → query & render lines (3-column listbox)
```

---

## How It Works

### 1. Entry Point

The main method called from `WWFActivity.display()`:

```java
public void render(MWFActivity activity)
```

This method is called every time the user selects an approval item in the left listbox.

### 2. Render Header

Displays 4 summary fields of the document at the top:

| UI Label | Default Content |
|----------|----------------|
| `lHdrCol1` | Document No. (`DocumentNo`, `Value`, `Name`) |
| `lHdrCol2` | Business Partner (`C_BPartner_ID>Name`) |
| `lHdrCol3` | Date (`DateOrdered`, `DateInvoiced`, etc.) |
| `lHdrCol4` | Total (`GrandTotal`, `TotalLines`) |

### 3. Render Lines

Displays a 3-column listbox containing document line items:

| Column | Default Content |
|--------|----------------|
| COL1 | Product description (`M_Product_ID>Name`) |
| COL2 | Qty (`QtyOrdered`, `QtyInvoiced`, `MovementQty`, etc.) |
| COL3 | Amount (`LineNetAmt`, `PriceActual`) |

---

## SysConfig Configuration

All configuration keys use the prefix: **`WF_DETAIL_<TableName>_<SUFFIX>`**

Example for table `C_Order`: `WF_DETAIL_C_Order_LINE_TABLE`

### Required Keys

The following keys **must be configured** for line items to be displayed:

| Key | Description | Example Value |
|-----|-------------|---------------|
| `WF_DETAIL_<Table>_LINE_TABLE` | Name of the line/detail table | `C_OrderLine` |
| `WF_DETAIL_<Table>_LINK_COL` | Foreign key column in the line table | `C_Order_ID` |

> **Note:** If `LINE_TABLE` is set to `-`, the listbox is hidden (for documents without lines, such as `C_Payment`).

### Optional Keys (With Defaults)

The following keys are **not required** — if left empty, the system uses default fallback values (see [Default Fallback Values](#default-fallback-values)).

#### Header Fields

| Key | Description |
|-----|-------------|
| `WF_DETAIL_<Table>_HDR_COL1` | Document number column |
| `WF_DETAIL_<Table>_HDR_COL1_LABEL` | Label for HDR_COL1 |
| `WF_DETAIL_<Table>_HDR_COL2` | Business Partner column |
| `WF_DETAIL_<Table>_HDR_COL2_LABEL` | Label for HDR_COL2 |
| `WF_DETAIL_<Table>_HDR_COL3` | Date column |
| `WF_DETAIL_<Table>_HDR_COL3_LABEL` | Label for HDR_COL3 |
| `WF_DETAIL_<Table>_HDR_COL4` | Total/value column |
| `WF_DETAIL_<Table>_HDR_COL4_LABEL` | Label for HDR_COL4 |
| `WF_DETAIL_<Table>_HDR_COL4_TYPE` | Data type for HDR_COL4 (`numeric` / `string`) |

#### Line Fields

| Key | Description |
|-----|-------------|
| `WF_DETAIL_<Table>_COL1` | Line description column |
| `WF_DETAIL_<Table>_COL1_LABEL` | Column 1 header in the listbox |
| `WF_DETAIL_<Table>_COL1_TYPE` | Data type (`string` / `numeric`) |
| `WF_DETAIL_<Table>_COL2` | Qty/quantity column |
| `WF_DETAIL_<Table>_COL2_LABEL` | Column 2 header in the listbox |
| `WF_DETAIL_<Table>_COL2_TYPE` | Data type (`string` / `numeric`) |
| `WF_DETAIL_<Table>_COL3` | Amount/value column |
| `WF_DETAIL_<Table>_COL3_LABEL` | Column 3 header in the listbox |
| `WF_DETAIL_<Table>_COL3_TYPE` | Data type (`string` / `numeric`) |
| `WF_DETAIL_<Table>_ORDER_BY` | Line sort order (SQL ORDER BY) |

### Value Format

#### 1. Comma-Separated Fallback List

A value may contain multiple column names separated by commas. The system will **try each one from left to right** and stop at the first non-null, non-empty value.

```
QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty
```

How it works (for `C_InvoiceLine`):
```
QtyOrdered  → null (column does not exist) → continue
QtyInvoiced → has value! → RETURN ← stops here
QtyEntered  → never reached
```

> **Important:** This is not an array — it is a **fallback list**. The first value found is used; the rest are ignored.

#### 2. FK Chain Lookup

To read a column from a related table via foreign key, use the `>` format:

```
C_BPartner_ID>Name
```

Multi-level FK chains are also supported:

```
C_BPartner_ID>C_BPartner_Location_ID>C_Location_ID>City
```

#### 3. Dash `-` Value

Use `-` to explicitly disable a feature:

- `LINE_TABLE = -` → hide the listbox (document has no lines)
- `COL3 = -` → leave the third column blank
- `HDR_COL4 = -` → do not display the total field

---

## Default Fallback Values

Default values are used automatically when a SysConfig key is not configured:

### Header

| Key Suffix | Default Value | Description |
|------------|--------------|-------------|
| `_HDR_COL1` | `DocumentNo,Value,Name` | Document number |
| `_HDR_COL1_LABEL` | `No. Dokumen` | |
| `_HDR_COL2` | `C_BPartner_ID>Name` | Business Partner |
| `_HDR_COL2_LABEL` | `Business Partner` | |
| `_HDR_COL3` | `DateOrdered,DateInvoiced,MovementDate,DateRequired,DateDoc,Created` | Date |
| `_HDR_COL3_LABEL` | `Tanggal` | |
| `_HDR_COL4` | `GrandTotal,TotalLines,-` | Grand Total |
| `_HDR_COL4_LABEL` | `Total` | |
| `_HDR_COL4_TYPE` | `numeric` | |

### Lines

| Key Suffix | Default Value | Description |
|------------|--------------|-------------|
| `_COL1` | `M_Product_ID>Name,Description,Name` | Description |
| `_COL1_LABEL` | `Deskripsi` | |
| `_COL1_TYPE` | `string` | |
| `_COL2` | `QtyOrdered,QtyInvoiced,MovementQty,QtyEntered,Qty` | Qty |
| `_COL2_LABEL` | `Qty` | |
| `_COL2_TYPE` | `numeric` | |
| `_COL3` | `LineNetAmt,PriceActual,-` | Amount |
| `_COL3_LABEL` | `Amount` | |
| `_COL3_TYPE` | `numeric` | |

> The default fallback values are designed to be generic enough to cover most standard iDempiere document tables. For tables with unique column names, it is recommended to configure the SysConfig keys explicitly.

---

## Supported Tables

The following tables are pre-configured via the 2Pack XML file (`WFDetail_SysConfig.xml`):

| Header Table | Line Table | Description |
|--------------|------------|-------------|
| `C_Order` | `C_OrderLine` | Sales / Purchase Order |
| `C_Invoice` | `C_InvoiceLine` | Invoice |
| `M_InOut` | `M_InOutLine` | Shipment / Receipt |
| `M_Requisition` | `M_RequisitionLine` | Purchase Requisition |
| `C_Payment` | `-` (no lines) | Payment |
| `C_BankStatement` | `C_BankStatementLine` | Bank Statement |
| `M_Movement` | `M_MovementLine` | Inventory Movement |
| `M_Inventory` | `M_InventoryLine` | Physical Inventory |
| `HR_Process` | `HR_Line` | Payroll Process |

---

## Configuration Examples

### C_Order (Minimal — required keys only)

```
WF_DETAIL_C_Order_LINE_TABLE  = C_OrderLine
WF_DETAIL_C_Order_LINK_COL    = C_Order_ID
```

All other columns will automatically use the default fallback values.

### C_Invoice (Custom — use QtyEntered before QtyInvoiced)

The default fallback for COL2 is `QtyOrdered,QtyInvoiced,...`, so `QtyInvoiced` would be picked first for `C_InvoiceLine`. To use `QtyEntered` instead:

```
WF_DETAIL_C_Invoice_LINE_TABLE  = C_InvoiceLine
WF_DETAIL_C_Invoice_LINK_COL    = C_Invoice_ID
WF_DETAIL_C_Invoice_COL2        = QtyEntered,QtyInvoiced
```

### C_Payment (No Lines)

```
WF_DETAIL_C_Payment_LINE_TABLE     = -
WF_DETAIL_C_Payment_HDR_COL4       = PayAmt
WF_DETAIL_C_Payment_HDR_COL4_LABEL = Payment Amount
```

### HR_Process (Custom Labels)

```
WF_DETAIL_HR_Process_LINE_TABLE   = HR_Line
WF_DETAIL_HR_Process_LINK_COL     = HR_Process_ID
WF_DETAIL_HR_Process_COL1         = HR_Concept_ID>Name
WF_DETAIL_HR_Process_COL1_LABEL   = Payroll Concept
WF_DETAIL_HR_Process_COL2         = Qty
WF_DETAIL_HR_Process_COL2_LABEL   = Quantity
WF_DETAIL_HR_Process_COL3         = Amount
WF_DETAIL_HR_Process_COL3_LABEL   = Value
```

---

## Column Resolution Logic

### `resolveColumnValue()` — Fallback List

```
Input   : "QtyOrdered,QtyInvoiced,MovementQty"
Process : try QtyOrdered → null? → try QtyInvoiced → has value → return
Output  : value of QtyInvoiced
```

Behaves like the **null coalescing operator** (`??`) but applied to PO column names.

### `resolveOneColumn()` — FK Chain

```
Input   : "C_BPartner_ID>Name"
Process : get C_BPartner_ID value from current PO
        → load C_BPartner PO with that ID
        → get Name column
Output  : business partner name
```

### `splitColumnDefs()` — Smart Splitting

Comma splitting is context-aware for FK chains:

```
Input  : "M_Product_ID>Name,Description"
Split  : ["M_Product_ID>Name", "Description"]
          ↑ FK chain kept intact  ↑ separate fallback
```

A `,` following a `>` is treated as part of the FK chain, not as a candidate separator.

### `formatByType()` — Type-Aware Formatting

| Type | Output Format |
|------|---------------|
| `numeric` | Number format with thousands separator (e.g. `1,500,000`) |
| `string` | Plain text, no formatting applied |

---

## Changelog

### v1.3.1 — Bug Fixes
- **Fix:** Moved the `"-".equals(lineTable)` guard before the `LINK_COL` check in `validateAndWarnConfig()` — previously it triggered an unnecessary warning when `LINE_TABLE="-"` (intentionally disabled).
- **Fix:** `resolveColumnValue()` now uses `splitColumnDefs()` — previously splitting directly on `","` caused FK chains like `C_BPartner_ID>Name,Description` to be split incorrectly.
- **Fix:** `renderListhead()` now uses `lstTxLines.appendChild(head)` — previously used `insertBefore()` which was incompatible with the installed ZK version.

### v1.3.0
- Added `COL1/COL2/COL3` support with dynamic labels and type-aware formatting.
- Multi-level FK lookup.
- Flexible column headers.

---

*This documentation covers the `org.nsoft.workflow.activities` plugin — iDempiere ERP customization.*
