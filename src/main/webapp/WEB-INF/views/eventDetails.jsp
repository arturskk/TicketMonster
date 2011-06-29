<%@ include file="/WEB-INF/views/include.jsp"%>
<div class="section">
	<div class="sectionHeader"><fmt:message key="event.heading"/></div>
	<div class="sectionContent">
		<c:out value=""/>
		<p>${event.description.active.content}<p/>
			<h3>Venues</h3>
				<table border="0">
					<c:forEach items="${venues}" var="venue">
						<tr>
							<td>
								<c:out value="${venue.name}"/>
							</td>
						</tr>
					</c:forEach>
			</table>
	</div>
</div>