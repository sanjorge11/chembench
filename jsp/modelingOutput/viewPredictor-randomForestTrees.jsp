<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="sx" uri="/struts-dojo-tags" %> 
<%@ page language="java" import="java.util.*" %>
	
<!-- Trees Page -->	
	<br />
		<p class="StandardTextDarkGray"><b><u>Random Forests</u></b></p>

		<p class="StandardTextDarkGray">
		<s:if test="randomForestTrees.size==0">
			No random forest trees were generated.<br/>
		</s:if>
		<s:elseif test="selectedPredictor.userName=='all-users'">
			<br/>Model information is not available for public predictors.<br/>
		</s:elseif>
		<s:else>
			<s:if test="isYRandomPage=='NO'">
				To generate the random forest predictor, a random forest is generated for each 
				train-test split, and the trees from each forest are combined together. This 
				page shows the trees from each of the train-test splits. 
			</s:if>
			<s:else>
				In y-Randomization modeling, Chembench attempts to create a second predictor from a copy of your
				data where the compound activities have been shuffled. Ideally, no trees with a high 
				R<sup>2</sup> will be produced. If the y-Randomized trees are similar to the real trees built on
				your data (see Trees tab), the predictor should be considered invalid and the dataset or parameters must
				be revised. Y-randomized trees are only created for validation purposes and are not used in predictions.
				
				This page shows the trees from y-randomized modeling for each train-test split.
			</s:else>	
			<br />
		</s:else>	
		</p>
		
	<!-- Table of Trees -->
		
		<table width="100%" align="center" class="sortable" id="randomForestTreesTable">
		<s:if test="selectedPredictor.activityType=='CONTINUOUS'">
		<s:if test="randomForestTrees.size!=0">
		<tr>
		<th class="TableRowText01narrow">Split Number</th>
		<th class="TableRowText01narrow">R<sup>2</sup></th>
		<th class="TableRowText01narrow">MSE</sup></th>
		<th class="TableRowText01narrow_unsortable" colspan="2">Descriptors Chosen</th>
		</tr>
		</s:if>
		<s:iterator value="randomForestTrees" status="treesStatus">
			<tr>
			<td class="TableRowText02narrow"><s:property value="treeFileName" /></td>
			<td class="TableRowText02narrow"><s:property value="r2" /></td>
			<td class="TableRowText02narrow"><s:property value="mse" /></td>
			<td class="TableRowText02narrow" colspan="2"><s:property value="descriptorsUsed" /></td>
			</tr> 
		</s:iterator>
		</s:if>
		
		<s:elseif test="selectedPredictor.activityType=='CATEGORY'">
		<s:if test="randomForestTrees.size!=0">
			<tr>
			<th class="TableRowText01narrow">Split Number</th>
			<th class="TableRowText01narrow_unsortable" colspan="3">Descriptors Chosen</th>
			</tr>
		</s:if>
		
		<s:iterator value="randomForestTrees" status="treesStatus">
			<tr>
				<td class="TableRowText02narrow"><s:property value="treeFileName" /></td>
				<td class="TableRowText02narrow" colspan="3"><s:property value="descriptorsUsed" /></td>
			</tr> 
		</s:iterator>
	
		</s:elseif>
		</table>
		
	<!-- End Table of Trees -->
	

		<s:if test="mostFrequentDescriptors!=''">
			<br />
			<p class="StandardTextDarkGray"><b><u>Descriptor Frequencies</u></b></p>
			<!--<p class="StandardTextDarkGray"><s:property value="mostFrequentDescriptors" /></p>-->
		</s:if>
		
<!-- End Trees Page -->