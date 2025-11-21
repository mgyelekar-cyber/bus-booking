import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


public class BusBookingGUI {

    private final BusService service = new BusService();
    private int nextBusId = 1;
    private int nextBookingId = 1000;

    // Swing components
    private JFrame frame;
    private JList<String> busList;
    private DefaultListModel<String> busListModel;
    private JTable bookingTable;
    private DefaultTableModel bookingTableModel;
    private JTextArea detailsArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BusBookingGUI().createAndShowGui());
    }

    public BusBookingGUI() {
        seedData();
    }

    private void seedData() {
        service.addBus(new Bus(nextBusId++, "Mumbai", "Pune", "2025-11-22 08:00", 40, 500));
        service.addBus(new Bus(nextBusId++, "Mumbai", "Goa", "2025-11-23 07:30", 30, 900));
        service.addBus(new Bus(nextBusId++, "Pune", "Nashik", "2025-11-20 06:00", 35, 300));
        service.addBus(new Bus(nextBusId++, "Mumbai", "Pune", "2025-11-21 14:00", 40, 550));
    }

    private void createAndShowGui() {
        frame = new JFrame("Bus Booking System - GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout(8, 8));

        // Left: Bus list
        busListModel = new DefaultListModel<>();
        refreshBusListModel();
        busList = new JList<>(busListModel);
        busList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane busListScroll = new JScrollPane(busList);
        busListScroll.setBorder(BorderFactory.createTitledBorder("Available Buses"));
        busList.setVisibleRowCount(10);

        // Right top: Details area
        detailsArea = new JTextArea(10, 40);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Bus Details / Seat Map"));

        // Right bottom: Booking table
        bookingTableModel = new DefaultTableModel(new Object[]{"BookingID", "Name", "Route", "Seats", "Fare", "Time"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        bookingTable = new JTable(bookingTableModel);
        JScrollPane bookingScroll = new JScrollPane(bookingTable);
        bookingScroll.setBorder(BorderFactory.createTitledBorder("Bookings"));

        // Buttons
        JButton btnViewDetails = new JButton("View Details");
        JButton btnBook = new JButton("Book Seats");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnCancel = new JButton("Cancel Booking");

        // Wire actions
        btnViewDetails.addActionListener(e -> onViewDetails());
        btnBook.addActionListener(e -> onBookSeats());
        btnRefresh.addActionListener(e -> refreshAll());
        btnCancel.addActionListener(e -> onCancelBooking());

        // Layout panels
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(busListScroll, BorderLayout.CENTER);
        JPanel leftButtons = new JPanel(new GridLayout(1, 3, 6, 6));
        leftButtons.add(btnViewDetails);
        leftButtons.add(btnBook);
        leftButtons.add(btnRefresh);
        leftPanel.add(leftButtons, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(6,6));
        rightPanel.add(detailsScroll, BorderLayout.NORTH);
        rightPanel.add(bookingScroll, BorderLayout.CENTER);
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtons.add(btnCancel);
        rightPanel.add(rightButtons, BorderLayout.SOUTH);

        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(rightPanel, BorderLayout.CENTER);

        // Fill booking table
        refreshBookingTable();

        // Show the first bus details by default if any
        if (!service.getAllBuses().isEmpty()) {
            busList.setSelectedIndex(0);
            showSelectedBusDetails();
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void refreshBusListModel() {
        busListModel.clear();
        for (Bus b : service.getAllBuses()) {
            busListModel.addElement(formatBusListEntry(b));
        }
    }

    private String formatBusListEntry(Bus b) {
        return String.format("%d: %s -> %s (%s)  Seats:%d Avail:%d Fare:%d",
                b.getId(), b.getSource(), b.getDestination(), b.getDeparture(), b.getTotalSeats(), b.getAvailableSeats(), b.getFarePerSeat());
    }

    private void refreshBookingTable() {
        bookingTableModel.setRowCount(0);
        for (Booking bk : service.getAllBookings()) {
            bookingTableModel.addRow(new Object[]{
                    bk.getBookingId(),
                    bk.getPassengerName(),
                    bk.getBus().getSource() + "-" + bk.getBus().getDestination(),
                    bk.getSeatsBooked(),
                    bk.getTotalFare(),
                    bk.getCreatedAt()
            });
        }
    }

    private void refreshAll() {
        refreshBusListModel();
        refreshBookingTable();
        showSelectedBusDetails();
    }

    private void onViewDetails() {
        showSelectedBusDetails();
    }

    private void showSelectedBusDetails() {
        int idx = busList.getSelectedIndex();
        if (idx < 0) {
            detailsArea.setText("Select a bus from the left to view details.");
            return;
        }
        // get bus id from the list model (we stored entries in order)
        Bus b = service.getAllBuses().get(idx);
        detailsArea.setText(buildBusDetailString(b));
    }

    private String buildBusDetailString(Bus b) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bus ID: ").append(b.getId()).append("\n");
        sb.append("Route: ").append(b.getSource()).append(" -> ").append(b.getDestination()).append("\n");
        sb.append("Departure: ").append(b.getDeparture()).append("\n");
        sb.append("Fare per seat: ").append(b.getFarePerSeat()).append("\n");
        sb.append("\nSeats (B=booked, .=free):\n");
        // layout 10 seats per row
        for (int i = 0; i < b.getTotalSeats(); i++) {
            sb.append(b.isSeatBooked(i) ? "B" : ".");
            if ((i + 1) % 10 == 0) {
                sb.append("   ").append((i - 8)).append("-").append(i + 1).append("\n");
            }
        }
        sb.append("\nAvailable seats: ").append(b.getAvailableSeats()).append("\n");
        return sb.toString();
    }

    private void onBookSeats() {
        int idx = busList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(frame, "Please select a bus to book.", "No Bus Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Bus bus = service.getAllBuses().get(idx);

        JPanel panel = new JPanel(new GridLayout(0,1,4,4));
        JTextField nameField = new JTextField();
        JTextField seatsField = new JTextField();
        panel.add(new JLabel("Passenger name:"));
        panel.add(nameField);
        panel.add(new JLabel("Number of seats to book (available: " + bus.getAvailableSeats() + "):"));
        panel.add(seatsField);

        int res = JOptionPane.showConfirmDialog(frame, panel, "Book Seats - Bus " + bus.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        int seatsToBook;
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            seatsToBook = Integer.parseInt(seatsField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Number of seats must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (seatsToBook <= 0) {
            JOptionPane.showMessageDialog(frame, "Seat count must be at least 1.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Booking bk = service.bookSeats(bus.getId(), name, seatsToBook, () -> nextBookingId++);
            JOptionPane.showMessageDialog(frame, "Booking successful!\n" + bk.toString(), "Booked", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(frame, "Booking failed: " + ise.getMessage(), "Booking Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelBooking() {
        int selRow = bookingTable.getSelectedRow();
        if (selRow < 0) {
            JOptionPane.showMessageDialog(frame, "Select a booking row to cancel.", "No Booking Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int bookingId = (int) bookingTableModel.getValueAt(selRow, 0);
        int confirm = JOptionPane.showConfirmDialog(frame, "Cancel booking " + bookingId + "?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        boolean ok = service.cancelBooking(bookingId);
        if (ok) {
            JOptionPane.showMessageDialog(frame, "Booking cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } else {
            JOptionPane.showMessageDialog(frame, "Booking not found (maybe already cancelled).", "Error", JOptionPane.ERROR_MESSAGE);
            refreshAll();
        }
    }

    // -------------------------
    // Model & Service (same idea as console version)
    // -------------------------
    static class Bus {
        private final int id;
        private final String source;
        private final String destination;
        private final String departure;
        private final int totalSeats;
        private final int farePerSeat;
        private final boolean[] seats;

        public Bus(int id, String source, String destination, String departure, int totalSeats, int farePerSeat) {
            this.id = id;
            this.source = source;
            this.destination = destination;
            this.departure = departure;
            this.totalSeats = totalSeats;
            this.farePerSeat = farePerSeat;
            this.seats = new boolean[totalSeats];
        }

        public int getId() { return id; }
        public String getSource() { return source; }
        public String getDestination() { return destination; }
        public String getDeparture() { return departure; }
        public int getTotalSeats() { return totalSeats; }
        public int getFarePerSeat() { return farePerSeat; }

        public synchronized int getAvailableSeats() {
            int c = 0;
            for (boolean s : seats) if (!s) c++;
            return c;
        }

        public synchronized List<Integer> allocateSeats(int count) {
            if (count > getAvailableSeats()) throw new IllegalStateException("Not enough seats available.");
            List<Integer> allocated = new ArrayList<>();
            for (int i = 0; i < seats.length && allocated.size() < count; i++) {
                if (!seats[i]) {
                    seats[i] = true;
                    allocated.add(i + 1);
                }
            }
            return allocated;
        }

        public synchronized boolean releaseSeats(List<Integer> seatNumbers) {
            boolean changed = false;
            for (Integer s : seatNumbers) {
                int idx = s - 1;
                if (idx >= 0 && idx < seats.length && seats[idx]) {
                    seats[idx] = false;
                    changed = true;
                }
            }
            return changed;
        }

        public boolean isSeatBooked(int zeroBasedIndex) {
            if (zeroBasedIndex < 0 || zeroBasedIndex >= seats.length) return false;
            return seats[zeroBasedIndex];
        }
    }

    static class Booking {
        private final int bookingId;
        private final Bus bus;
        private final String passengerName;
        private final List<Integer> seatNumbers;
        private final int totalFare;
        private final String createdAt;

        public Booking(int bookingId, Bus bus, String passengerName, List<Integer> seatNumbers) {
            this.bookingId = bookingId;
            this.bus = bus;
            this.passengerName = passengerName;
            this.seatNumbers = new ArrayList<>(seatNumbers);
            this.totalFare = seatNumbers.size() * bus.getFarePerSeat();
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        public int getBookingId() { return bookingId; }
        public Bus getBus() { return bus; }
        public String getPassengerName() { return passengerName; }
        public List<Integer> getSeatNumbers() { return Collections.unmodifiableList(seatNumbers); }
        public int getSeatsBooked() { return seatNumbers.size(); }
        public int getTotalFare() { return totalFare; }
        public String getCreatedAt() { return createdAt; }

        @Override
        public String toString() {
            return "BookingID: " + bookingId + " | Name: " + passengerName + " | Bus: " + bus.getSource() + "-" + bus.getDestination()
                    + " | Seats: " + seatNumbers + " | Fare: " + totalFare + " | Time: " + createdAt;
        }
    }

    interface IdProvider { int next(); }

    static class BusService {
        private final Map<Integer, Bus> buses = new LinkedHashMap<>();
        private final Map<Integer, Booking> bookings = new LinkedHashMap<>();

        public void addBus(Bus bus) { buses.put(bus.getId(), bus); }

        public List<Bus> getAllBuses() {
            return new ArrayList<>(buses.values());
        }

        public Bus getBusById(int id) { return buses.get(id); }

        public synchronized Booking bookSeats(int busId, String passengerName, int seatsCount, IdProvider idProvider) {
            Bus bus = buses.get(busId);
            if (bus == null) throw new IllegalStateException("Bus does not exist.");
            if (bus.getAvailableSeats() < seatsCount) throw new IllegalStateException("Not enough seats available.");
            List<Integer> allocated = bus.allocateSeats(seatsCount);
            int bookingId = idProvider.next();
            Booking booking = new Booking(bookingId, bus, passengerName, allocated);
            bookings.put(bookingId, booking);
            return booking;
        }

        public synchronized boolean cancelBooking(int bookingId) {
            Booking bk = bookings.remove(bookingId);
            if (bk == null) return false;
            Bus bus = bk.getBus();
            bus.releaseSeats(bk.getSeatNumbers());
            return true;
        }

        public List<Booking> getAllBookings() {
            return new ArrayList<>(bookings.values());
        }
    }
}
