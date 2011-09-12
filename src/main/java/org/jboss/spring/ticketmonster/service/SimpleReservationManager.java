package org.jboss.spring.ticketmonster.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.spring.ticketmonster.domain.BookingRequest;
import org.jboss.spring.ticketmonster.domain.BookingState;
import org.jboss.spring.ticketmonster.domain.CacheKey;
import org.jboss.spring.ticketmonster.domain.PriceCategory;
import org.jboss.spring.ticketmonster.domain.PriceCategoryRequest;
import org.jboss.spring.ticketmonster.domain.RowReservation;
import org.jboss.spring.ticketmonster.domain.SeatBlock;
import org.jboss.spring.ticketmonster.domain.Section;
import org.jboss.spring.ticketmonster.domain.SectionRequest;
import org.jboss.spring.ticketmonster.domain.SectionRow;
import org.jboss.spring.ticketmonster.domain.User;
import org.jboss.spring.ticketmonster.repo.ShowDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Implementation of the ReservationManager interface. 
 *
 * @author Ryan Bradley
 *
 */

public class SimpleReservationManager implements ReservationManager {

	@Autowired
	private ShowDao showDao;
	
	@Autowired
	private CacheManager cacheManager;
	
	@Autowired
	private BookingState bookingState;
	
	private static final boolean TEMPORARY = false;
	
	protected final Log logger = LogFactory.getLog(getClass());

	public BookingState getBookingState() {
		return bookingState;
	}
	
	public void setBookingState(BookingState bookingState) {
		this.bookingState = bookingState;
	}

	public List<SectionRequest> createSectionRequests(BookingRequest booking) {
		
		boolean found = false;
		
		User user = new User();
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		user.setUsername(username);
		bookingState.setUser(user);
		
		List<PriceCategoryRequest> categoryRequests = booking.getCategoryRequests();
		List<SectionRequest> sectionRequests = new ArrayList<SectionRequest>();
		
		for(PriceCategoryRequest categoryRequest : categoryRequests) {
			for(SectionRequest sectRequest : sectionRequests) {
				if(sectRequest.getSectionId() == categoryRequest.getPriceCategory().getSection().getId()) {
					found = true;
					sectRequest.addQuantity(categoryRequest);
					continue;
				}
			}
			if(found == false) {
				SectionRequest sectionRequest = new SectionRequest();
				sectionRequest.setQuantity(categoryRequest);
				sectionRequest.setSectionId(categoryRequest);
				sectionRequests.add(sectionRequest);
			}
			found = false;
		}
		
		return sectionRequests;
	}

	public boolean findContiguousSeats(Long showId, Long sectionId, int quantity) {
		ConcurrentMapCache reservationsCache = (ConcurrentMapCache) cacheManager.getCache("reservations");
		
		Section section = showDao.findSection(sectionId);
		List<SectionRow> rows = showDao.getRowsBySection(section, quantity);
		
		RowReservation reservation = new RowReservation();
		
		for(SectionRow row : rows) {
			CacheKey key = new CacheKey(showId, row.getId());
			
			if(reservationsCache.get(key) != null) {
				reservation = (RowReservation) reservationsCache.get(key).get();
			}
			else {
				reservation.setCapacity(row.getCapacity());
				reservation.setReservedSeats(new LinkedList<SeatBlock>());
			}
			
			LinkedList<SeatBlock> reservedSeats = reservation.getReservedSeats();
			
			// Case for the first seat reservation in a certain row.
			
			if(reservedSeats.isEmpty()) {
				SeatBlock block = new SeatBlock();
				block.setStartSeat(1);
				block.setEndSeat(quantity);
				block.setPurchased(TEMPORARY);
				block.setKey(key);
				reservedSeats.add(block);
				bookingState.addSeatBlock(block);
				reservation.setReservedSeats(reservedSeats);
				reservationsCache.put(key, reservation);
				return true;
			}
			
			// Case for the second seat reservation in a certain row.
			
			if(reservedSeats.size() == 1) {
				SeatBlock frontBlock = reservedSeats.get(0);
				if(row.getCapacity() - frontBlock.getEndSeat() >= quantity) {
					SeatBlock block = this.reserveSeats(frontBlock, quantity, key);
					reservedSeats.add(block);
					bookingState.addSeatBlock(block);
					reservation.setReservedSeats(reservedSeats);
					reservationsCache.put(key, reservation);
					return true;
				}
			}
			
			// General case for seat reservation in a certain row.
			
			for(SeatBlock firstBlock : reservedSeats) {
				
				// Check if the block is the last block in the list.
				
				if(firstBlock == reservedSeats.getLast()) {
					if(row.getCapacity() - firstBlock.getEndSeat() >= quantity) {
						SeatBlock block = this.reserveSeats(firstBlock, quantity, key);
						reservedSeats.add(block);
						bookingState.addSeatBlock(block);
						reservation.setReservedSeats(reservedSeats);
						reservationsCache.put(key, reservation);
						return true;
					}
					
					break;
				}
				SeatBlock secondBlock = reservedSeats.get(reservedSeats.indexOf(firstBlock)+1);
				
				if(firstBlock.getStartSeat() - secondBlock.getEndSeat() >= quantity) {
					SeatBlock block = this.reserveSeats(firstBlock, quantity, key);
					reservedSeats.add(reservedSeats.indexOf(secondBlock)+1, block);
					bookingState.addSeatBlock(block);
					reservation.setReservedSeats(reservedSeats);
					reservationsCache.put(key, reservation);
					return true;
				}
			}
			
		}

		return false;
	}
	
	public SeatBlock reserveSeats(SeatBlock frontBlock, int quantity, CacheKey key) {
		SeatBlock block = new SeatBlock();
		
		block.setStartSeat(frontBlock.getEndSeat()+1);
		block.setEndSeat(block.getStartSeat()+quantity-1);
		block.setKey(key);
		block.setPurchased(TEMPORARY);
		
		return block;
	}
	
	public boolean updateSeatReservation(Long showId, Long sectionId, int quantity) {
		boolean success = false;
				
		if(quantity < 0) {
			return false;
		}
			
		if(quantity == 0) {
			logger.info("Found an allocation with quantity 0.");
			logger.info("There are " + bookingState.getReserved().size() + " reservations for the current session.");
			Long rowId = bookingState.reservationExists(showId, sectionId);
			if(rowId != 0) {
				logger.info("Removing reservation in row " + rowId + ".");
				this.removeSeatReservation(showId, rowId);
				return true;
			}
			if(rowId == 0) {
				return true;
			}
		}
		
		Long rowId = bookingState.reservationExists(showId, sectionId);
		if(rowId > 0) {				
			SeatBlock block = this.update(showId, rowId, quantity);
			if(block != null) {
				return true;
			}
			else {
				return false;
			}
		}
		
		success = this.findContiguousSeats(showId, sectionId, quantity);
		return success;
	}
	
	public SeatBlock update(Long showId, Long rowId, int quantity) {
		ConcurrentMapCache reservationsCache = (ConcurrentMapCache) cacheManager.getCache("reservations");
		logger.info("Updating the seats of an already reserved block.");
		
		CacheKey key = new CacheKey(showId, rowId);
		RowReservation reservation = new RowReservation();
		LinkedList<SeatBlock> reservedSeats = new LinkedList<SeatBlock>();
		
		if(reservationsCache.get(key) != null) {
			reservation = (RowReservation) reservationsCache.get(key).get();
			reservedSeats = reservation.getReservedSeats();
		}
		
		for(SeatBlock block : reservedSeats) {
			if(bookingState.getReserved().contains(block)) {
				if(reservedSeats.getLast() == block) {
					if((block.getStartSeat()+quantity-1) <= reservation.getCapacity()) {
						block.setEndSeat(block.getStartSeat()+quantity-1);
						reservation.setReservedSeats(reservedSeats);
						reservationsCache.put(key, reservation);
						return block;
					}
					else {
						return null;
					}
				}
				else {
					SeatBlock nextBlock = reservedSeats.get(reservedSeats.indexOf(block)+1);
					if((nextBlock.getStartSeat()-block.getStartSeat()) <= quantity) {
						block.setEndSeat(block.getStartSeat()+quantity-1);
						reservation.setReservedSeats(reservedSeats);
						reservationsCache.put(key, reservation);
						return block;
					}
					else {
						return null;
					}
				}
			}
		}
		
		return null;
	}
	
	public void removeSeatReservation(Long showId, Long rowId)	{
		logger.info("Entering removeSeatReservation() method");
		ConcurrentMapCache reservationsCache = (ConcurrentMapCache) cacheManager.getCache("reservations");
		CacheKey key = new CacheKey(showId, rowId);
		
		if(reservationsCache.get(key) == null) {
			logger.info("Did not find a reservation in " + rowId +".");
			return;
		}
		
		RowReservation reservation = (RowReservation) reservationsCache.get(key).get();
		LinkedList<SeatBlock> reservedSeats = reservation.getReservedSeats();
		
		for(SeatBlock block : reservedSeats) {
			if(this.bookingState.getReserved().contains(block)) {
				logger.info("Found a reservation in " + rowId + ", reservation will be removed.");
				reservedSeats.remove(block);
				this.bookingState.removeReservation(block);
				reservation.setReservedSeats(reservedSeats);
				reservationsCache.put(key, reservation);
				logger.info("Reservation should be removed, and cache should be updated appropriately.");
				return;
			}
		}
		
		return;
	}
	
	public void updateCategoryRequest(Long showId, Long priceCategoryId, int quantity) {
		PriceCategory category = showDao.findPriceCategory(priceCategoryId);
		PriceCategoryRequest categoryRequest = new PriceCategoryRequest(category);
		categoryRequest.setQuantity(quantity);
		this.getBookingState().addCategoryRequest(categoryRequest);
		return;
	}

}
