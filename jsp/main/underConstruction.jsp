<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib uri="/tags/struts-bean" prefix="bean"%>
<%@ taglib uri="/tags/struts-logic" prefix="logic"%>
<%@ taglib uri="/tags/struts-html" prefix="html"%>
<%@ taglib uri="/tags/struts-nested" prefix="nested"%>
<jsp:useBean class="edu.unc.ceccr.persistence.User" id="user"	scope="session"></jsp:useBean>
<%@ page import="edu.unc.ceccr.global.Constants" %>
<html:html>
<head>
<title>C-CHEMBENCH | CECCR Base</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link href="theme/standard.css" rel="stylesheet"	type="text/css" />
<link href="theme/links.css" rel="stylesheet" type="text/css" />
<link rel="icon"  href="theme/img/mml.ico" type="image/ico"></link>
<link rel="SHORTCUT ICON" href="theme/img/mml.ico" ></link>
<link href="theme/dynamicTab.css" rel="stylesheet" type="text/css" />
<script src="javascript/script.js"></script>

</head>
<body onload="setTabToCeccrBase();">
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0"><tr><td><%@include file="/jsp/main/header.jsp" %></td></tr></table>
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0"><tr><td><%@include file="/jsp/main/centralNavigationBar.jsp" %></td></tr></table>
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0">
	<tr>
	
	
		<span id="maincontent">
		<td height="557" colspan="5" valign="top"
			background="theme/img/backgrlibrary.jpg">
		<p class="StandardTextDarkGray">&nbsp;</p>
		<p class="StandardTextDarkGray"><strong></strong>
		</p>
		<p class="StandardTextDarkGrayParagraph">COMING SOON.<br>This page is currently under construction. </p>
		<p class="StandardTextBlack">&nbsp;</p>
		</td>
		</span>
	</tr>
	<tr>
	<%@include file ="/jsp/main/footer.jsp" %>
</body>
</html:html>
