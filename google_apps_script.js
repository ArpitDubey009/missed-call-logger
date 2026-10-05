/**
 * MissedCall Logger — Google Apps Script
 * ========================================
 * Deploy as Web App (Execute as: Me, Access: Anyone)
 * 
 * AUTO-SCALING FEATURES:
 * - Auto-creates monthly sheets (Oct-2026, Nov-2026, etc.)
 * - Auto-increments serial numbers globally
 * - Auto-updates Dashboard with daily/weekly/monthly stats
 * - Auto-archives data older than 6 months
 */

// ============== CONFIGURATION ==============
var SPREADSHEET_ID = ''; // Leave empty to use the bound spreadsheet
var DASHBOARD_SHEET_NAME = 'Dashboard';
var ARCHIVE_MONTHS = 6; // Auto-archive after 6 months

// ============== MAIN HANDLER ==============

/**
 * Handles incoming POST requests from the Android app
 */
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    
    var phoneNumber = data.phoneNumber || 'Unknown';
    var callerName  = data.callerName  || 'Unknown';
    var date        = data.date        || getCurrentDate();
    var time        = data.time        || getCurrentTime();
    var callType    = data.callType    || 'Missed';
    
    // Get or create the monthly sheet
    var sheetName = getMonthlySheetName(date);
    var sheet = getOrCreateMonthlySheet(sheetName);
    
    // Get the next global serial number
    var srNo = getNextGlobalSerialNumber();
    
    // Append the missed call row
    sheet.appendRow([srNo, phoneNumber, callerName, date, time, callType]);
    
    // Auto-format the new row
    var lastRow = sheet.getLastRow();
    formatRow(sheet, lastRow);
    
    // Update the Dashboard
    updateDashboard();
    
    // Return success
    return ContentService
      .createTextOutput(JSON.stringify({
        status: 'success',
        message: 'Call logged successfully',
        srNo: srNo,
        sheet: sheetName
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    return ContentService
      .createTextOutput(JSON.stringify({
        status: 'error',
        message: error.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Handles GET requests (for testing the connection)
 */
function doGet(e) {
  var ss = getSpreadsheet();
  var totalCalls = getTotalMissedCalls();
  
  return ContentService
    .createTextOutput(JSON.stringify({
      status: 'active',
      message: 'MissedCall Logger is running',
      totalCallsLogged: totalCalls,
      spreadsheetName: ss.getName()
    }))
    .setMimeType(ContentService.MimeType.JSON);
}

// ============== AUTO-SCALING: MONTHLY SHEETS ==============

/**
 * Returns sheet name like "Oct-2026" from a date string
 */
function getMonthlySheetName(dateStr) {
  var months = ['Jan','Feb','Mar','Apr','May','Jun',
                'Jul','Aug','Sep','Oct','Nov','Dec'];
  
  var parts = dateStr.split('-'); // Expected: 2026-10-05
  var year = parts[0];
  var monthIndex = parseInt(parts[1], 10) - 1;
  
  return months[monthIndex] + '-' + year;
}

/**
 * Gets existing monthly sheet or creates a new one with headers
 */
function getOrCreateMonthlySheet(sheetName) {
  var ss = getSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  
  if (!sheet) {
    // Create new monthly sheet
    sheet = ss.insertSheet(sheetName);
    
    // Add headers
    var headers = ['Sr. No', 'Phone Number', 'Caller Name', 'Date', 'Time', 'Call Type'];
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    
    // Format headers
    var headerRange = sheet.getRange(1, 1, 1, headers.length);
    headerRange.setFontWeight('bold');
    headerRange.setBackground('#1a73e8');
    headerRange.setFontColor('#ffffff');
    headerRange.setFontSize(11);
    headerRange.setHorizontalAlignment('center');
    
    // Set column widths
    sheet.setColumnWidth(1, 80);   // Sr. No
    sheet.setColumnWidth(2, 180);  // Phone Number
    sheet.setColumnWidth(3, 180);  // Caller Name
    sheet.setColumnWidth(4, 130);  // Date
    sheet.setColumnWidth(5, 130);  // Time
    sheet.setColumnWidth(6, 100);  // Call Type
    
    // Freeze header row
    sheet.setFrozenRows(1);
    
    Logger.log('Created new monthly sheet: ' + sheetName);
  }
  
  return sheet;
}

// ============== AUTO-SCALING: SERIAL NUMBERS ==============

/**
 * Gets the next global serial number across ALL monthly sheets
 */
function getNextGlobalSerialNumber() {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var maxSrNo = 0;
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    // Skip Dashboard and any non-monthly sheets
    if (name === DASHBOARD_SHEET_NAME || name === 'Sheet1') continue;
    
    var lastRow = sheets[i].getLastRow();
    if (lastRow <= 1) continue; // Only header row
    
    // Check the last Sr. No in column A
    var lastSrNo = sheets[i].getRange(lastRow, 1).getValue();
    if (typeof lastSrNo === 'number' && lastSrNo > maxSrNo) {
      maxSrNo = lastSrNo;
    }
  }
  
  return maxSrNo + 1;
}

// ============== AUTO-SCALING: DASHBOARD ==============

/**
 * Auto-updates the Dashboard sheet with statistics
 */
function updateDashboard() {
  var ss = getSpreadsheet();
  var dashboard = ss.getSheetByName(DASHBOARD_SHEET_NAME);
  
  if (!dashboard) {
    dashboard = ss.insertSheet(DASHBOARD_SHEET_NAME, 0); // First tab
  }
  
  // Clear existing content
  dashboard.clear();
  
  // ---- Title ----
  dashboard.getRange('A1').setValue('📊 Missed Call Logger — Dashboard');
  dashboard.getRange('A1').setFontSize(18).setFontWeight('bold').setFontColor('#1a73e8');
  dashboard.getRange('A2').setValue('Last Updated: ' + new Date().toLocaleString('en-IN'));
  dashboard.getRange('A2').setFontSize(10).setFontColor('#666666');
  
  // ---- Overall Stats ----
  var row = 4;
  dashboard.getRange('A' + row).setValue('📈 Overall Statistics').setFontSize(14).setFontWeight('bold');
  row++;
  
  var totalCalls = getTotalMissedCalls();
  var todayCalls = getTodayMissedCalls();
  var thisWeekCalls = getThisWeekMissedCalls();
  var thisMonthCalls = getThisMonthMissedCalls();
  var topCallers = getTopCallers(5);
  
  var statsHeaders = ['Metric', 'Value'];
  var statsData = [
    ['Total Missed Calls (All Time)', totalCalls],
    ['Missed Calls Today', todayCalls],
    ['Missed Calls This Week', thisWeekCalls],
    ['Missed Calls This Month', thisMonthCalls],
    ['Active Monthly Sheets', getMonthlySheetCount()]
  ];
  
  dashboard.getRange(row, 1, 1, 2).setValues([statsHeaders]);
  dashboard.getRange(row, 1, 1, 2).setFontWeight('bold').setBackground('#e8f0fe').setFontColor('#1a73e8');
  row++;
  
  dashboard.getRange(row, 1, statsData.length, 2).setValues(statsData);
  row += statsData.length + 1;
  
  // ---- Top Callers ----
  dashboard.getRange('A' + row).setValue('📞 Top Missed Callers').setFontSize(14).setFontWeight('bold');
  row++;
  
  var callerHeaders = ['Rank', 'Phone Number / Name', 'Missed Count'];
  dashboard.getRange(row, 1, 1, 3).setValues([callerHeaders]);
  dashboard.getRange(row, 1, 1, 3).setFontWeight('bold').setBackground('#e8f0fe').setFontColor('#1a73e8');
  row++;
  
  if (topCallers.length > 0) {
    dashboard.getRange(row, 1, topCallers.length, 3).setValues(topCallers);
    row += topCallers.length;
  } else {
    dashboard.getRange('A' + row).setValue('No data yet');
    row++;
  }
  row++;
  
  // ---- Monthly Breakdown ----
  dashboard.getRange('A' + row).setValue('📅 Monthly Breakdown').setFontSize(14).setFontWeight('bold');
  row++;
  
  var monthlyBreakdown = getMonthlyBreakdown();
  var monthHeaders = ['Month', 'Total Missed Calls'];
  dashboard.getRange(row, 1, 1, 2).setValues([monthHeaders]);
  dashboard.getRange(row, 1, 1, 2).setFontWeight('bold').setBackground('#e8f0fe').setFontColor('#1a73e8');
  row++;
  
  if (monthlyBreakdown.length > 0) {
    dashboard.getRange(row, 1, monthlyBreakdown.length, 2).setValues(monthlyBreakdown);
  }
  
  // Auto-resize columns
  dashboard.setColumnWidth(1, 280);
  dashboard.setColumnWidth(2, 200);
  dashboard.setColumnWidth(3, 150);
}

// ============== STATISTICS HELPERS ==============

function getTotalMissedCalls() {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var total = 0;
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name === DASHBOARD_SHEET_NAME || name === 'Sheet1') continue;
    total += Math.max(0, sheets[i].getLastRow() - 1); // Subtract header
  }
  return total;
}

function getTodayMissedCalls() {
  return getCallsByDateRange(0);
}

function getThisWeekMissedCalls() {
  return getCallsByDateRange(7);
}

function getThisMonthMissedCalls() {
  return getCallsByDateRange(30);
}

function getCallsByDateRange(daysBack) {
  var ss = getSpreadsheet();
  var today = new Date();
  var startDate = new Date(today);
  startDate.setDate(today.getDate() - daysBack);
  
  var sheetName = getMonthlySheetName(getCurrentDate());
  var sheet = ss.getSheetByName(sheetName);
  
  if (!sheet || sheet.getLastRow() <= 1) return 0;
  
  var data = sheet.getRange(2, 4, sheet.getLastRow() - 1, 1).getValues(); // Date column
  var count = 0;
  
  for (var i = 0; i < data.length; i++) {
    var callDate = new Date(data[i][0]);
    if (callDate >= startDate) count++;
  }
  
  return count;
}

function getTopCallers(limit) {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var callerMap = {};
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name === DASHBOARD_SHEET_NAME || name === 'Sheet1') continue;
    
    var lastRow = sheets[i].getLastRow();
    if (lastRow <= 1) continue;
    
    var data = sheets[i].getRange(2, 2, lastRow - 1, 2).getValues(); // Phone + Name
    for (var j = 0; j < data.length; j++) {
      var key = data[j][0]; // phone number
      var callerName = data[j][1] || 'Unknown';
      var display = key + (callerName !== 'Unknown' ? ' (' + callerName + ')' : '');
      
      if (!callerMap[display]) callerMap[display] = 0;
      callerMap[display]++;
    }
  }
  
  // Sort by count descending
  var sorted = Object.keys(callerMap).map(function(key) {
    return [key, callerMap[key]];
  }).sort(function(a, b) { return b[1] - a[1]; });
  
  var result = [];
  for (var k = 0; k < Math.min(limit, sorted.length); k++) {
    result.push([k + 1, sorted[k][0], sorted[k][1]]);
  }
  
  return result;
}

function getMonthlyBreakdown() {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var result = [];
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name === DASHBOARD_SHEET_NAME || name === 'Sheet1') continue;
    
    var count = Math.max(0, sheets[i].getLastRow() - 1);
    if (count > 0) {
      result.push([name, count]);
    }
  }
  
  return result;
}

function getMonthlySheetCount() {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var count = 0;
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name !== DASHBOARD_SHEET_NAME && name !== 'Sheet1') count++;
  }
  return count;
}

// ============== AUTO-ARCHIVE ==============

/**
 * Run this monthly via a time-based trigger to archive old sheets
 * Set up: Triggers → Add Trigger → archiveOldSheets → Month timer
 */
function archiveOldSheets() {
  var ss = getSpreadsheet();
  var sheets = ss.getSheets();
  var cutoffDate = new Date();
  cutoffDate.setMonth(cutoffDate.getMonth() - ARCHIVE_MONTHS);
  
  var months = {
    'Jan':0,'Feb':1,'Mar':2,'Apr':3,'May':4,'Jun':5,
    'Jul':6,'Aug':7,'Sep':8,'Oct':9,'Nov':10,'Dec':11
  };
  
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name === DASHBOARD_SHEET_NAME || name === 'Sheet1') continue;
    
    var parts = name.split('-');
    if (parts.length !== 2) continue;
    
    var monthStr = parts[0];
    var yearStr = parts[1];
    
    if (months[monthStr] === undefined) continue;
    
    var sheetDate = new Date(parseInt(yearStr), months[monthStr], 1);
    
    if (sheetDate < cutoffDate) {
      // Hide old sheets instead of deleting (safe archive)
      sheets[i].hideSheet();
      Logger.log('Archived (hidden): ' + name);
    }
  }
}

// ============== UTILITY FUNCTIONS ==============

function getSpreadsheet() {
  if (SPREADSHEET_ID && SPREADSHEET_ID !== '') {
    return SpreadsheetApp.openById(SPREADSHEET_ID);
  }
  return SpreadsheetApp.getActiveSpreadsheet();
}

function getCurrentDate() {
  var now = new Date();
  var year = now.getFullYear();
  var month = String(now.getMonth() + 1).padStart(2, '0');
  var day = String(now.getDate()).padStart(2, '0');
  return year + '-' + month + '-' + day;
}

function getCurrentTime() {
  var now = new Date();
  return now.toLocaleTimeString('en-IN', { hour12: true });
}

function formatRow(sheet, rowNum) {
  var range = sheet.getRange(rowNum, 1, 1, 6);
  range.setHorizontalAlignment('center');
  range.setVerticalAlignment('middle');
  range.setFontSize(10);
  
  // Alternate row coloring
  if (rowNum % 2 === 0) {
    range.setBackground('#f8f9fa');
  } else {
    range.setBackground('#ffffff');
  }
}

// ============== INITIAL SETUP ==============

/**
 * Run this once to set up the initial Dashboard
 */
function initialSetup() {
  updateDashboard();
  Logger.log('Initial setup complete!');
}

/**
 * Run this once to set up auto-archive trigger
 */
function setupAutoArchiveTrigger() {
  ScriptApp.newTrigger('archiveOldSheets')
    .timeBased()
    .onMonthDay(1)  // 1st of every month
    .atHour(2)      // 2 AM
    .create();
  Logger.log('Auto-archive trigger created!');
}
