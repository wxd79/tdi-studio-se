---
version: 7.0.1
module: https://talend.poolparty.biz/coretaxonomy/42
product:
- https://talend.poolparty.biz/coretaxonomy/23
---

# TPS-3957

| Info             | Value |
| ---------------- | ---------------- |
| Patch Name       | Patch\_20200423_TPS-3957\_v1-7.0.1 |
| Release Date     | 2020-04-23 |
| Target Version   | 20180411\_1414-V7.0.1 |
| Product affected | Talend Studio |

## Introduction

This is a self-contained patch.

**NOTE**: For information on how to obtain this patch, reach out to your Support contact at Talend.

## Fixed issues

This patch contains the following fixes:

- TPS-3957 [7.0.1]tjdbcinput Dynamic Schema not preserving special characters(TDI-44051)

## Prerequisites

Consider the following requirements for your system:

- Talend Studio 7.0.1 must be installed.
- Installed Patch_20190518_TPS-3103_v1-7.0.1 in advance


## Installation

### Installing the patch using Software update

1) Logon TAC and switch to Configuration->Software Update, then enter the correct values and save referring to the documentation: https://help.talend.com/reader/f7Em9WV_cPm2RRywucSN0Q/j9x5iXV~vyxMlUafnDejaQ

2) Switch to Software update page, where the new patch will be listed. The patch can be downloaded from here into the nexus repository.

3) On Studio Side: Logon Studio with remote mode, on the logon page the Update button is displayed: click this button to install the patch.

### Installing the patch using Talend Studio

1) Create a folder named "patches" under your studio installer directory and copy the patch .zip file to this folder.

2) Restart your studio: a window pops up, then click OK to install the patch, or restart the commandline and the patch will be installed automatically.

### Installing the patch using Commandline

Execute the following commands:

1. Talend-Studio-win-x86_64.exe -nosplash -application org.talend.commandline.CommandLine -consoleLog -data commandline-workspace startServer -p 8002 --talendDebug
2. initRemote {tac_url} -ul {TAC login username} -up {TAC login password}
3. checkAndUpdate -tu {TAC login username} -tup {TAC login password}

## Uninstallation
Backup the Affected files list below. Uninstall the patch by restore the backup files.

## Affected files for this patch

The following files are installed by this patch:

- {Talend\_Studio\_path}/plugins/plugins/org.talend.designer.codegen\_7.0.1.20190514\_0827-patch/jet\_stub/generic/component\_util\_indexedrecord\_to\_rowstruct.javajet
- {Talend\_Studio\_path}/plugins/org.talend.desinger.routines.tisprovider\_7.0.1.20180411\_1414/resources/java/routines/system/DynamicUtils.java