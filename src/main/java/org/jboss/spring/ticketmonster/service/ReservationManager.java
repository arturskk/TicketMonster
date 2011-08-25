package org.jboss.spring.ticketmonster.service;

import java.util.List;

import org.jboss.spring.ticketmonster.domain.BookingRequest;
import org.jboss.spring.ticketmonster.domain.BookingState;
import org.jboss.spring.ticketmonster.domain.CacheKey;
import org.jboss.spring.ticketmonster.domain.SeatBlock;
import org.jboss.spring.ticketmonster.domain.SectionRequest;

/**
 * Interface containing methods to reserve seating that has been allocated.
 * 
 * @author Ryan Bradley
 *
 */

public interface ReservationManager {
	
	BookingState getBookingState();
	
	List<SectionRequest> createSectionRequests(BookingRequest booking);
	
	boolean findContiguousSeats(Long showId, Long sectionId, int quantity);
	
	SeatBlock allocateSeats(SeatBlock frontBlock, int quantity, CacheKey key);
	
	boolean updateSeatAllocation(Long showId, Long sectionId, int quantity);
	
	SeatBlock update(Long showId, Long rowId, int quantity);
	
	void removeSeatAllocation(Long showId, Long rowId);
}
