<%@ include file="/WEB-INF/views/include.jsp"%>
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

        function refreshTimes(venueId, eventId) {
            baseUrl = '<c:url value="/shows.htm?"/>'
            jQuery.getJSON(baseUrl + "eventId=" + eventId + "&venueId=" + venueId, function (result) {
                $("select#times").empty()
                jQuery.each(result, function (index, value) {
                    $("select#times").append("<option value='" + value.showId + "'>" + prettyDate( new Date(value.date)) + "</option>");
                });
            });
        }
    </script>
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
								<a href="<c:url value="../venues/${venue.id}.htm"/>">${venue.name}</a>
                                <a href="#" onclick="refreshTimes(${venue.id}, ${event.id});">Show Times</a>
							</td>
						</tr>
					</c:forEach>
			</table>
            <p/>
            <h3>Show times</h3>
            <select id="times"/>
	</div>
</div>