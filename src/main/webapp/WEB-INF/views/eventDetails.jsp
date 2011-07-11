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
            baseUrl = '<c:url value="/shows.htm?"/>';
            jQuery.getJSON(baseUrl + "eventId=" + eventId + "&venueId=" + venueId, function (result) {
                $("select#times").empty();
                jQuery.each(result, function (index, value) {
                    $("select#times").append("<option value='" + value.showId + "'>" + prettyDate( new Date(value.date)) + "</option>");
                });
            });
        }

		function getVenueDetails(venueId) {
            baseUrl = '<c:url value= "/venues/[venueId].htm" />';
        	jQuery.getJSON(baseUrl.replace("[venueId]", venueId), function (result) {
				$("div#venueDetails").empty();
	            $("div#venueDetails").append(result.address + "<p/>" + result.description.active.content);
		    });
        }

		 function getPriceCategories(eventId, venueId) {
	            baseUrl = '<c:url value= "/categories.htm?"/>';
	            jQuery.getJSON(baseUrl + "eventId=" + eventId + "&venueId=" + venueId, function (result) {
	                $("#priceCategories").accordion("destroy");
	                $("#priceCategories").empty();
	                currentSection = 0;
	                var html = "";
	                jQuery.each(result, function (index, value) {
	                    if (currentSection != value.section.id) {
	                        if (currentSection != 0) {
	                            html += ("</ul></div>");
	                        }
	                        html += ("<h3><a href='#'>" + value.section.name + "</a></h3>");
	                        html += ("<div><ul>");
	                        currentSection = value.section.id;
	                    }
	                    html += ("<li>" + value.category.description + " - $" + value.price + "</li>");
	                });
	                if (currentSection != 0) {
	                    html += ("</ul></div>");
	                }
	                $("#priceCategories").append($(html));
	                $("#priceCategories").accordion();
	            });

	        }

</script>
    
<div class="section">

	<div class="sectionHeader"><fmt:message key="event.heading"/></div>
	<div class="sectionContent">
		<c:out value=""/>
		<p>${event.description.active.content}<p/>
			<h3>Venues</h3>
            <p/>
            <select id="venues">
            <c:forEach items="${venues}" var="venue">
				<option value='${venue.id}'>${venue.name}</option>
			</c:forEach>
            </select>
            <div class="sectionContent" id="venueDetails"></div>
            <p/>
            <h3>Show Times</h3>
            <select id="times"></select>
            <p/>
            <br><br><br><br>
            <div class="sectionContent" id="priceCategories"></div>
	</div>
</div>

<script type="text/javascript">
$("select#venues").change(function () {
	$("select#venues option:selected ").each(function() {
        refreshTimes($(this).val(), <c:out value="${event.id}"/>);
        getVenueDetails($(this).val());
        getPriceCategories(<c:out value="${event.id}"/>, $(this).val());
        $("div#priceCategories").accordion();
    });
}).change();

	$(function() {
		$( "#accordion" ).accordion();
	});
</script>