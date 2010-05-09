<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="sx" uri="/struts-dojo-tags" %> 
<%@page language="java" import="java.util.*" %>

<html>
<head>
	<sx:head debug="false" cache="false" compressed="true" />
<title>C-CHEMBENCH | My Bench</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link href="theme/standard.css" rel="stylesheet" type="text/css" />
<link href="theme/links.css" rel="stylesheet" type="text/css" />
<link href="theme/dynamicTab.css" rel="stylesheet" type="text/css" />
<link rel="icon"  href="theme/img/mml.ico" type="image/ico"></link>
<link rel="SHORTCUT ICON" href="theme/img/mml.ico" ></link>

<script src="javascript/script.js"></script>
<script language="JavaScript" src="javascript/sortableTable.js"></script>
</head>
<body onload="setTabToMyBench();">
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0"><tr><td><%@include file="/jsp/main/header.jsp" %></td></tr></table>
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0"><tr><td><%@include file="/jsp/main/centralNavigationBar.jsp" %></td></tr></table>
<table width="924" border="0" align="center" cellpadding="0" cellspacing="0">
	<tr>
		<td height="557" colspan="5" valign="top"
			background="theme/img/backgrindex.jpg" STYLE="background-repeat: no-repeat;">
		<p class="StandardTextDarkGrayParagraph"><br><br><b>My Bench</b> <br><br>
		<table width="465" border="0" cellspacing="0" cellpadding="0">
        <tr><td>
        <p class="StandardTextDarkGrayParagraph">
		Every dataset, predictor, and prediction you have created on C-Chembench is available on this page. 
		You can also track progress of all the running jobs using the job queue.
		</p>
		</td></tr>
		</table>
		<table width="100%" border="0" cellspacing="0" cellpadding="20">
  <tr>
    <td></td>
  </tr>
</table>

	<!-- Queued, Local, and LSF Jobs -->
	<a name="jobs"></a>
	<table width="94%" frame="border" rules="none" align="center" cellpadding="0" cellspacing="4" colspan="2">
		<tbody>			
		<tr>
			<td height="24" align="left" colspan="2">
			<p class="StandardTextDarkGrayParagraph2">
			<br /><b>Job Queue</b>
			</p></td>
		</tr>
		<tr>
			<td colspan="2">
			<div class="StandardTextDarkGrayParagraph"><i>Running jobs from all Chembench users are displayed below. Use the REFRESH STATUS button to update the list. 
			Other users can see your jobs while they are running, but only you can access your completed datasets, predictors, and predictions.</i><br /></div></td>
		 </tr>	
		 <tr><td colspan="2">
		<form action="jobs">
			<div class="StandardTextDarkGrayParagraph"><button type="submit">REFRESH STATUS</button></div>
		</form>
		</td></tr>
		<tr><td colspan="2">&nbsp; </td></tr>
		
		<!-- Queued (incomingJobs) -->
		<tr><td colspan="2">
			<div class="StandardTextDarkGrayParagraph">
			<b>Unassigned Jobs: </b>
			</div></td>
		</tr>
		<tr><td colspan="2"><div class="StandardTextDarkGrayParagraph">
		<s:if test="! incomingJobs.isEmpty()">
		<table class="sortable">
			<tr>
				<th class="TableRowText01">Name</th>
				<th class="TableRowText01">Owner</th>
				<th class="TableRowText01">Job Type</th>
				<th class="TableRowText01">Number of Compounds</th>
				<th class="TableRowText01">Number of Models</th>
				<th class="TableRowText01">Time Created</th>
				<th class="TableRowText01">Status</th>
				<th class="TableRowText01_unsortable">Cancel</th>
			</tr>
			<s:iterator value="incomingJobs">
				<tr>
				<td class="TableRowText02"><s:property value="jobName" /></td>
				<td class="TableRowText02"><s:property value="userName" /></td>
				<td class="TableRowText02"><s:property value="jobType" /></td>
				<td class="TableRowText02"><s:property value="numCompounds" /></td>
				<td class="TableRowText02"><s:if test="jobTypeString!='dataset'"><s:property value="numModels" /></s:if><s:else>N/A</s:else></td>
				<td class="TableRowText02"><s:date name="timeCreated" format="yyyy-MM-dd HH:mm" /></td>
				<td class="TableRowText02"><b><s:property value="message" /><b></td>
				<td class="TableRowText02">
					<s:if test="adminUser"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:if>
					<s:elseif test="user.userName==userName"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:elseif>
				</td>
				</tr> 
			</s:iterator>
			<tr><td colspan="2">&nbsp;</td></tr>
		</table>
		</s:if>
		<s:else>
		<table>
			<tr><td>
			<div class="StandardTextDarkGray">(No jobs are waiting to be assigned.)</div>
			</td></tr>
			<tr><td>&nbsp;</td></tr>
		</table>
		</s:else>
		
		<!-- Local Jobs -->
		<tr><td colspan="2">
			<div class="StandardTextDarkGrayParagraph">
			<b>Jobs on Local Queue: </b>
			</div></td>
		</tr>
		<tr><td colspan="2"><div class="StandardTextDarkGrayParagraph">
		<s:if test="! localJobs.isEmpty()">
		<table class="sortable">
			<tr>
				<th class="TableRowText01">Name</th>
				<th class="TableRowText01">Owner</th>
				<th class="TableRowText01">Job Type</th>
				<th class="TableRowText01">Number of Compounds</th>
				<th class="TableRowText01">Number of Models</th>
				<th class="TableRowText01">Time Created</th>
				<th class="TableRowText01">Status</th>
				<th class="TableRowText01_unsortable">Cancel</th>
			</tr>
			<s:iterator value="localJobs">
				<tr>
				<td class="TableRowText02"><s:property value="jobName" /></td>
				<td class="TableRowText02"><s:property value="userName" /></td>
				<td class="TableRowText02"><s:property value="jobType" /></td>
				<td class="TableRowText02"><s:property value="numCompounds" /></td>
				<td class="TableRowText02"><s:if test="jobTypeString!='dataset'"><s:property value="numModels" /></s:if><s:else>N/A</s:else></td>
				<td class="TableRowText02"><s:date name="timeCreated" format="yyyy-MM-dd HH:mm" /></td>
				<td class="TableRowText02"><b><s:property value="message" /><b></td>
				<td class="TableRowText02">
					<s:if test="adminUser"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:if>
					<s:elseif test="user.userName==userName"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:elseif></td>
				</td>
				</tr> 
			</s:iterator>
			<tr><td colspan="2">&nbsp;</td></tr>
		</table>
		</s:if>
		<s:else>
		<table>
			<tr><td>
			<div class="StandardTextDarkGray">(The local processing queue is empty.)</div>
			<tr><td>&nbsp;</td></tr>
			</td></tr>
		</table>
		</s:else>
		
		
		<!-- LSF Jobs -->
		<tr><td colspan="2">
			<div class="StandardTextDarkGrayParagraph">
			<b>Jobs on LSF Queue: </b>
			</div></td>
		</tr>
		<tr><td colspan="2"><div class="StandardTextDarkGrayParagraph">
		<s:if test="! lsfJobs.isEmpty()">
		<table class="sortable">
			<tr>
				<th class="TableRowText01">Name</th>
				<th class="TableRowText01">Owner</th>
				<th class="TableRowText01">Job Type</th>
				<th class="TableRowText01">Number of Compounds</th>
				<th class="TableRowText01">Number of Models</th>
				<th class="TableRowText01">Time Created</th>
				<th class="TableRowText01">Status</th>
				<th class="TableRowText01_unsortable">Cancel</th>
			</tr>
			<s:iterator value="lsfJobs">
				<tr>
				<td class="TableRowText02"><s:property value="jobName" /></td>
				<td class="TableRowText02"><s:property value="userName" /></td>
				<td class="TableRowText02"><s:property value="jobType" /></td>
				<td class="TableRowText02"><s:property value="numCompounds" /></td>
				<td class="TableRowText02"><s:if test="jobTypeString!='dataset'"><s:property value="numModels" /></s:if><s:else>N/A</s:else></td>
				<td class="TableRowText02"><s:date name="timeCreated" format="yyyy-MM-dd HH:mm" /></td>
				<td class="TableRowText02"><b><s:property value="message" /><b></td>
				<td class="TableRowText02">
					<s:if test="adminUser"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:if>
					<s:elseif test="user.userName==userName"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:elseif>
				</td>
				</tr> 
			</s:iterator>
			<tr><td colspan="2">&nbsp;</td></tr>
		</table>
		</s:if>
		<s:else>
		<table>
			<tr><td>
			<div class="StandardTextDarkGray">(The LSF queue is empty.)</div>
			</td></tr>
			<tr><td>&nbsp;</td></tr>
		</table>
		</s:else>
		
		<!-- Error Jobs -->
		<s:if test="! errorJobs.isEmpty()">
		<tr><td colspan="2">
			<div class="StandardTextDarkGrayParagraph">
			<b>Jobs with errors: </b>
			</div></td>
		</tr>
		<tr><td colspan="2"><div class="StandardTextDarkGrayParagraph">
		<table class="sortable">
			<tr>
				<th class="TableRowText01">Name</th>
				<th class="TableRowText01">Owner</th>
				<th class="TableRowText01">Job Type</th>
				<th class="TableRowText01">Number of Compounds</th>
				<th class="TableRowText01">Number of Models</th>
				<th class="TableRowText01">Time Created</th>
				<th class="TableRowText01">Status</th>
				<th class="TableRowText01_unsortable">Cancel</th>
			</tr>
			<s:iterator value="lsfJobs">
				<tr>
				<td class="TableRowText02"><s:property value="jobName" /></td>
				<td class="TableRowText02"><s:property value="userName" /></td>
				<td class="TableRowText02"><s:property value="jobType" /></td>
				<td class="TableRowText02"><s:property value="numCompounds" /></td>
				<td class="TableRowText02"><s:if test="jobTypeString!='dataset'"><s:property value="numModels" /></s:if><s:else>N/A</s:else></td>
				<td class="TableRowText02"><s:date name="timeCreated" format="yyyy-MM-dd HH:mm" /></td>
				<td class="TableRowText02"><b><s:property value="message" /><b></td>
				<td class="TableRowText02">
					<s:if test="adminUser"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:if>
					<s:elseif test="user.userName==userName"><a href="deleteJob?id=<s:property value="id" />#jobs">cancel</a></s:elseif>
				</td>
				</tr> 
			</s:iterator>
			<tr><td colspan="2">&nbsp;</td></tr>
		</table>
		</s:if>
		
	</table>
	<br />
		
	<!-- Finished Dataset Jobs -->
	<a name="datasets"></a>
	<table width="94%" frame="border" rules="none" align="center" cellpadding="0" cellspacing="4" colspan="2">
		<tbody>			
		<tr>
			<td height="24" align="left" colspan="2">
			<p class="StandardTextDarkGrayParagraph2">
			<br /><b>Datasets</b>
			</p></td>
		</tr>
		<tr>
			<td colspan="2">
			<div class="StandardTextDarkGrayParagraph"><i>Click on the name of dataset to visualize it.</i><br />
			
			<table class="sortable" id="datasets">
				<tr>
					<th class="TableRowText01">Name</th>
					<th class="TableRowText01">Date Created</th>
					<th class="TableRowText01">Number of Compounds</th>
					<th class="TableRowText01">Type</th>
					<th class="TableRowText01">Public/Private</th>
					<th class="TableRowText01_unsortable">Download</th>
					<th class="TableRowText01_unsortable">Delete</th>
				</tr>
				<s:iterator value="userDatasets">
					<s:if test="hasBeenViewed=='YES'">
					<tr class="TableRowText02">
					</s:if>
					<s:else>
					<tr class="TableRowText03">
					</s:else>
					
					<td align="center">
						<a href="viewDataset?id=<s:property value="fileId" />">
						<s:property value="fileName" />
						</a>
					</td>
					<td><s:date name="createdTime" format="yyyy-MM-dd HH:mm" /></td>
					<td><s:property value="numCompound" /></td>
					<td><s:property value="modelType" /></td>
					
					<s:if test="userName=='_all'">
					<td>Public</td>
					<td><a href="datasetFilesServlet.do?datasetName=<s:property value="fileName" />&user=all-users">download</a></td>
					<td><!-- dataset is public, so no delete option --></td>
					</s:if>
					<s:else>
					<td>Private</td>
					<td><a href="datasetFilesServlet.do?datasetName=<s:property value="fileName" />&user=<s:property value="user.userName" />">download</a></td>
					<td><a onclick="return confirmDelete('dataset')" href="deleteDataset?id=<s:property value="fileId" />#datasets">delete</a></td>
					</s:else>
					
					</tr> 
				</s:iterator>
				<br />
				<br />
			</table>
			</div>
			</td>
		 </tr>	
	</tbody>
	</table>
	<br />

	<!-- Finished Modeling Jobs -->
	<a name="predictors"></a>
	<table width="94%" frame="border" rules="none" align="center" cellpadding="0" cellspacing="4" colspan="2">
		<tbody>			
		<tr>
			<td height="24" align="left" colspan="2">
			<p class="StandardTextDarkGrayParagraph2">
			<br /><b>Predictors</b>
			</p></td>
		</tr>
		<tr>
			<td colspan="2">
			<div class="StandardTextDarkGrayParagraph"><i>Click on the name of a predictor to analyze the modeling results.</i><br />
			
			<table class="sortable" id="predictors">
				<tr>
					<th class="TableRowText01">Name</th>
					<th class="TableRowText01">Date Created</th>
					<th class="TableRowText01">Modeling Method</th>
					<th class="TableRowText01">Descriptor Type</th>
					<th class="TableRowText01">Dataset</th>
					<th class="TableRowText01">Public/Private</th>
					<th class="TableRowText01_unsortable">Download</th>
					<th class="TableRowText01_unsortable">Delete</th>
				</tr>
				<s:iterator value="userPredictors">
					<s:if test="hasBeenViewed=='YES'">
					<tr class="TableRowText02">
					</s:if>
					<s:else>
					<tr class="TableRowText03">
					</s:else>
					
					<s:url id="predictorLink" value="/viewPredictor" includeParams="none">
						<s:param name="id" value='predictorId' />
					</s:url>
					<td><s:a href="%{predictorLink}"><s:property value="name" /></s:a></td>
					<td><s:date name="dateCreated" format="yyyy-MM-dd HH:mm" /></td>
					<td><s:property value="modelMethod" /></td>
					<td><s:property value="descriptorGeneration" /></td>
					<td><s:property value="datasetDisplay" /></td>
					<td><s:if test="userName=='_all'">Public</s:if><s:else>Private</s:else></td>
					<td><a href="projectFilesServlet?project=<s:property value='name' />&user=<s:property value="userName" />&projectType=modeling">download</a></td>
					<td><s:if test="userName=='_all'"></s:if><s:else><a onclick="return confirmDelete('predictor')" href="deletePredictor?id=<s:property value="predictorId" />#predictors">delete</a></s:else></td>
					</tr> 
				</s:iterator>
				<br />
				<br />
			</table>
			
			</div>
			</td>
		 </tr>	
	</tbody>
	</table>
	<br />
	
	<!-- Finished Prediction Jobs -->
	<a name="predictions"></a>
	<table width="94%" frame="border" rules="none" align="center" cellpadding="0" cellspacing="4" colspan="2">
		<tbody>			
		<tr>
			<td height="24" align="left" colspan="2">
			<p class="StandardTextDarkGrayParagraph2">
			<br /><b>Predictions</b>
			</p></td>
		</tr>
		<tr>
			<td colspan="2">
			<div class="StandardTextDarkGrayParagraph"><i>Click on the name of a prediction to see the results.</i><br />
			<table class="sortable" id="predictions">
				<tr>
					<th class="TableRowText01">Name</th>
					<th class="TableRowText01">Date Created</th>
					<th class="TableRowText01">Dataset</th>
					<th class="TableRowText01">Predictor</th>
					<th class="TableRowText01_unsortable">Download</th>
					<th class="TableRowText01_unsortable">Delete</th>
				</tr>
				<s:iterator value="userPredictions">
					<s:if test="hasBeenViewed=='YES'">
					<tr class="TableRowText02">
					</s:if>
					<s:else>
					<tr class="TableRowText03">
					</s:else>
					
					<s:url id="predictionLink" value="/viewPrediction" includeParams="none">
						<s:param name="id" value='predictionId' />
					</s:url>
					<td><s:a href="%{predictionLink}"><s:property value="jobName" /></s:a></td>
					<td><s:date name="dateCreated" format="yyyy-MM-dd HH:mm" /></td>
					<td><s:property value="datasetDisplay" /></td>
					<td><s:property value="predictorNames" /></td>
					<td><a href="projectFilesServlet?project=<s:property value='jobName' />&user=<s:property value='userName' />&projectType=prediction">download</a></td>
					<td><a onclick="return confirmDelete('prediction')" href="deletePrediction?id=<s:property value="predictionId" />#predictions">delete</a></td>
					</tr> 
				</s:iterator>
				<br />
				<br />
			</table>
			</div>
			</td>
		 </tr>	
	</tbody>
	</table>
	<br />
	</div>
	<br>
	</td></tr>
</table>
		</td>
	</tr>
	<tr>
	<%@include file ="/jsp/main/footer.jsp" %>
</body>