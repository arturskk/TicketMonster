package org.jboss.spring.ticketmonster.mvc;

import java.util.List;

import org.jboss.spring.ticketmonster.domain.BookingRequest;
import org.jboss.spring.ticketmonster.domain.PriceCategory;
import org.jboss.spring.ticketmonster.domain.PriceCategoryRequest;
import org.jboss.spring.ticketmonster.domain.Show;
import org.jboss.spring.ticketmonster.repo.ShowDao;
import org.jboss.spring.ticketmonster.service.AllocationManager;
import org.jboss.spring.ticketmonster.service.ReservationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/bookings")
public class BookingFormController {
	
	@Autowired
	private ShowDao showDao;
	
	@Autowired
	private AllocationManager allocationManager;
	
	@Autowired
	private ReservationManager reservationManager;
	
	@RequestMapping(value = "/{id}", method=RequestMethod.GET)
	public String viewShow(@PathVariable("id") Long id, Model model) {
		Show show = showDao.getShow(id);
		model.addAttribute("show", show);
		
		Long eventId = show.getEvent().getId();
		Long venueId = show.getVenue().getId();
		
		List<PriceCategory> categories = showDao.getCategories(eventId, venueId);
		model.addAttribute("categories", categories);
		
		BookingRequest bookingRequest = new BookingRequest();
		bookingRequest.setShowId(id);
		bookingRequest.initializeRequest(categories);
		model.addAttribute("bookingRequest", bookingRequest);
		
		return "showDetails";		
	}
	
	@RequestMapping(value="/submit", method=RequestMethod.POST)
	public String onSubmit(Model model) {
		allocationManager.finalizeReservations(allocationManager.getBookingState().getReserved());

		model.addAttribute("allocations", allocationManager.getBookingState().getAllocations());
		Double total = allocationManager.calculateTotal(allocationManager.getBookingState().getCategoryRequests());
		model.addAttribute("user", allocationManager.getBookingState().getUser());		
		model.addAttribute("total", total);
		reservationManager.getBookingState().clear();
		
		return "checkout";
	}
	
	@RequestMapping(value = "/allocate", method=RequestMethod.GET, produces = "application/json")
	public @ResponseBody boolean updateReservation(Long showId, Long priceCategoryId, int quantity) {
		boolean success = false;
		int sectionQuantity = 0;

		PriceCategory category = showDao.findPriceCategory(priceCategoryId);
		Long sectionId = category.getSection().getId();

		/*
		 * Should add a testReservation method before I commit the change to the PriceCategoryRequest object.
		 */		

		List<PriceCategoryRequest> categoryRequests = reservationManager.getBookingState().getCategoryRequests();
		reservationManager.updateCategoryRequest(showId, priceCategoryId, quantity);
		for(PriceCategoryRequest categoryRequest : categoryRequests) {
			if(categoryRequest.getPriceCategory().getSection().getId().intValue() == sectionId.intValue()) {
				sectionQuantity += categoryRequest.getQuantity();
			}
		}
		
/*		List<SectionRequest> sectionRequests = reservationManager.createSectionRequests(bookingRequest);
		List<PriceCategoryRequest> priceCategoryRequests = bookingRequest.getCategoryRequests();
		
		for(SectionRequest sectionRequest : sectionRequests) {
			if(sectionRequest.getQuantity() < 0) {
				continue;
			}
			
			else if(sectionRequest.getQuantity() == 0) {
				found = reservationManager.getBookingState().reservationExists(showId, sectionRequest.getSectionId());
				if(found == true) {
					reservationManager.removeSeatReservation(showId, sectionRequest.getSectionId());
					success = true;
				}
				else {
					success = true;
				}
			}
			
			else {
				found = reservationManager.getBookingState().reservationExists(showId, sectionRequest.getSectionId());
				if(found == true) {
					success = reservationManager.updateSeatReservation(showId, sectionRequest.getSectionId(), sectionRequest.getQuantity());
				}
				else {
					success = reservationManager.findContiguousSeats(showId, sectionRequest.getSectionId(), sectionRequest.getQuantity());
				}
			}
*/			
		success = reservationManager.updateSeatReservation(showId, sectionId, sectionQuantity);
			
/*			for(PriceCategoryRequest priceCategoryRequest : priceCategoryRequests) {
				if((priceCategoryRequest.getPriceCategory().getSection().getId() == sectionRequest.getSectionId()) && success == true) {
					reservationManager.getBookingState().addCategoryRequest(priceCategoryRequest);
				}
			}
		}*/
		
		return success;
	}
	
}
