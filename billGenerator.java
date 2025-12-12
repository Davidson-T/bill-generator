import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Product {
    private int id;
    private String name;
    private int quantity;
    private double price;
    
    public Product(int id, String name, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    
    // Setters
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

public class billGenerator extends JFrame {
    private JTable productTable, cartTable;
    private DefaultTableModel productModel, cartModel;
    private JTextArea billArea;
    private JLabel totalLabel;
    private List<Product> products = new ArrayList<>();
    private List<CartItem> cart = new ArrayList<>();
    private double totalAmount = 0.0;
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/billing_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    
    public billGenerator() {
        setTitle("Bill Generator - Java Swing + MySQL");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initDatabase(); 
        initComponents();
        loadProducts();
    }
    
    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", DB_USER, DB_PASS)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS billing_db");
            conn.setCatalog("billing_db");
            
            String createTable = "CREATE TABLE IF NOT EXISTS products (" +
                "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "product_quantity INT DEFAULT 0, " +
                "price DECIMAL(10,2) NOT NULL)";
            stmt.execute(createTable);
            
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO products (product_name, product_quantity, price) VALUES " +
                    "('Laptop', 10, 50000.00), ('Mouse', 50, 500.00), ('Keyboard', 25, 1500.00)");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    private void initComponents() {
        
        JPanel productPanel = new JPanel(new BorderLayout());
        productModel = new DefaultTableModel(new String[]{"ID", "Name", "Stock", "Price"}, 0);
        productTable = new JTable(productModel);
        productPanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        
        
        JPanel topBtnPanel = new JPanel();
        JButton addProductBtn = new JButton("➕ Add Product");
        topBtnPanel.add(addProductBtn);
        productPanel.add(topBtnPanel, BorderLayout.NORTH);
        
        
        JPanel productBtnPanel = new JPanel();
        JButton addToCartBtn = new JButton("Add to Cart");
        JTextField qtyField = new JTextField(5);
        qtyField.setText("1");
        
        productBtnPanel.add(new JLabel("Qty:"));
        productBtnPanel.add(qtyField);
        productBtnPanel.add(addToCartBtn);
        productPanel.add(productBtnPanel, BorderLayout.SOUTH);
        
        
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartModel = new DefaultTableModel(new String[]{"Name", "Qty", "Price", "Total"}, 0);
        cartTable = new JTable(cartModel);
        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        
        JPanel cartBtnPanel = new JPanel();
        JButton clearCartBtn = new JButton("Clear Cart");
        JButton generateBillBtn = new JButton("Generate Bill");
        totalLabel = new JLabel("Total: ₹0.00");
        
        cartBtnPanel.add(clearCartBtn);
        cartBtnPanel.add(generateBillBtn);
        cartBtnPanel.add(totalLabel);
        cartPanel.add(cartBtnPanel, BorderLayout.SOUTH);
        
        // Bill Area
        billArea = new JTextArea();
        billArea.setEditable(false);
        billArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, productPanel, cartPanel);
        add(splitPane, BorderLayout.CENTER);
        add(new JScrollPane(billArea), BorderLayout.SOUTH);
        
        
        addProductBtn.addActionListener(e -> showAddProductDialog());
        addToCartBtn.addActionListener(e -> addToCart(qtyField.getText()));
        clearCartBtn.addActionListener(e -> clearCart());
        generateBillBtn.addActionListener(e -> generateBill());
    }
    
    
    private void showAddProductDialog() {
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(20);
        JTextField qtyField = new JTextField(10);
        JTextField priceField = new JTextField(10);
        
        Object[] fields = {
            "Product ID:", idField,
            "Product Name:", nameField,
            "Quantity:", qtyField,
            "Price:", priceField
        };
        
        int result = JOptionPane.showConfirmDialog(this, fields, "Add New Product", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                int qty = Integer.parseInt(qtyField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim());
                
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Product name cannot be empty!");
                    return;
                }
                
                
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    String sql = "INSERT INTO products (product_id, product_name, product_quantity, price) VALUES (?, ?, ?, ?)";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, id);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, qty);
                    pstmt.setDouble(4, price);
                    int rows = pstmt.executeUpdate();
                    
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Product added successfully!");
                        loadProducts(); 
                    }
                }
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for ID, Qty, and Price!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage() + 
                    "\nNote: Product ID must be unique!");
                ex.printStackTrace();
            }
        }
    }
    
    private void loadProducts() {
        productModel.setRowCount(0);
        products.clear();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
            
            while (rs.next()) {
                Product p = new Product(rs.getInt("product_id"), rs.getString("product_name"),
                    rs.getInt("product_quantity"), rs.getDouble("price"));
                products.add(p);
                productModel.addRow(new Object[]{p.getId(), p.getName(), p.getQuantity(), "₹" + p.getPrice()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private void addToCart(String qtyText) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product!");
            return;
        }
        
        try {
            int qty = Integer.parseInt(qtyText);
            Product product = products.get(selectedRow);
            
            if (product.getQuantity() < qty) {
                JOptionPane.showMessageDialog(this, "Insufficient stock! Available: " + product.getQuantity());
                return;
            }
            
            // Update stock in database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String sql = "UPDATE products SET product_quantity = product_quantity - ? WHERE product_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, qty);
                pstmt.setInt(2, product.getId());
                pstmt.executeUpdate();
            }
            
            // Add to cart
            CartItem item = new CartItem(product.getName(), qty, product.getPrice());
            cart.add(item);
            totalAmount += qty * product.getPrice();
            
            cartModel.addRow(new Object[]{item.name, item.quantity, "₹" + item.price, "₹" + item.getTotal()});
            totalLabel.setText("Total: ₹" + String.format("%.2f", totalAmount));
            
            loadProducts(); // Refresh product table
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid quantity!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private void clearCart() {
        cart.clear();
        totalAmount = 0.0;
        cartModel.setRowCount(0);
        totalLabel.setText("Total: ₹0.00");
        billArea.setText("");
    }
    
    private void generateBill() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }
        
        StringBuilder bill = new StringBuilder("=== BILL GENERATOR ===\n\n");
        bill.append(String.format("%-20s %-5s %-8s %-10s\n", "Product", "Qty", "Price", "Total"));
        bill.append("------------------------------------------------\n");
        
        for (CartItem item : cart) {
            double itemTotal = item.quantity * item.price;
            bill.append(String.format("%-20s %-5d ₹%-7.2f ₹%-8.2f\n", 
                item.name, item.quantity, item.price, itemTotal));
        }
        
        bill.append("------------------------------------------------\n");
        bill.append(String.format("%-38s ₹%.2f\n", "GRAND TOTAL:", totalAmount));
        bill.append("\nThank you for shopping!\n");
        bill.append("Date: " + java.time.LocalDateTime.now());
        
        billArea.setText(bill.toString());
        JOptionPane.showMessageDialog(this, "Bill generated successfully!");
    }
    
    private static class CartItem {
        String name;
        int quantity;
        double price;
        
        CartItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
        
        double getTotal() {
            return quantity * price;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new billGenerator().setVisible(true);
        });
    }
}
