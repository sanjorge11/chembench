<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="sx" uri="/struts-dojo-tags" %> 
<%@ page language="java" import="java.util.*" %>


<script language="javascript" src="javascript/datasetscripts.js"></script>
<s:div>
	<table width="100%" align="center" cellpadding="0" cellspacing="4" colspan="2">
		<tbody>	
		<tr>
			<td width="100%" height="24" align="left" colspan="2">
			<br />
			<p class="StandardTextDarkGrayParagraph2">
			<b>Upload Dataset for Prediction Only</b>
			</p>
			</td>
		</tr>	
		<tr><td colspan="2"><table>
	  <tr>
		<td colspan="2">
		<div class="StandardTextDarkGrayParagraph"><i>A dataset will be created from the <a href="">SDF</a> file you supply.</i><br /></div>
	    </td>
	  </tr>		
	  <tr>
	    <td><b class="StandardTextDarkGrayParagraph">SDF File:</b></td>
	    <td align="left"><input id="loadSdf" name="sdfFile" type="file"/></td>
	  </tr>
  	  </table>
    </td></tr></tbody></table>
</s:div>