<%@ include file="/WEB-INF/views/include.jsp" %>

<script type="text/javascript">
        var dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
        var monthNames = ["January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"];

        function prettyDate(val) {
            return dayNames[val.getDay()] + " " +
                    val.getDate() + " " +
                    monthNames[val.getMonth()] + " " +
                    val.getFullYear() + " @ " +
                    zeropad(val.getHours(), 2) + ":" +
                    zeropad(val.getMinutes(), 2);
        }

        function zeropad(val, digits) {
            val = val + "";
            while (val.length < digits) val = "0" + val;
            return val;
        }
</script>

<div class="section">
	<div class="sectionHeader"><fmt:message key="bookings.heading"/></div>
	<div class="sectionContent" id="showDate"></div>
	<div class="sectionContent">
			<c:out value=""/>
			<p>${show.event.description.active.content}</p>
			
			<p>${show.venue.name}<br>
			${show.venue.address}
			${show.venue.description.active.content}</p>
	</div>
	<div class="sectionContent" id="priceCategories">
		<table border="0">
		<c:forEach items="${categories}" var="category">
			<c:if test="${category.section.name != lastSection}">
				<tr>			
				<td><h3><a href="#"><c:out value="${category.section.name}"/></a></h3></td>
				</tr>
			</c:if>
			<c:set var="lastSection" value="${category.section.name}"/>			
			<tr>
				<td><c:out value="${category.category.description} - ${category.price}"/></td>
			</tr>
		</c:forEach>
		</table>
	</div>
</div>

<script type="text/javascript">
	$("div#showDate").append(prettyDate(new Date(<c:out value="${show.showDate.time}"/>)));
</script>
