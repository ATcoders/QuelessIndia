import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class QueLessIndia {

static Connection con;

static CardLayout card = new CardLayout();
static JPanel mainPanel = new JPanel(card);

static int userId = -1;

public static void main(String[] args){

connectDB();
createTables();
createUI();

}

static void connectDB(){

try{

Class.forName("com.mysql.cj.jdbc.Driver");

con = DriverManager.getConnection(
"jdbc:mysql://localhost:3306/queless_india",
"root",
""
);

}catch(Exception e){
e.printStackTrace();
}

}

static void createTables(){

try{

Statement st = con.createStatement();

st.executeUpdate("CREATE TABLE IF NOT EXISTS users(" +
"user_id INT AUTO_INCREMENT PRIMARY KEY," +
"name VARCHAR(100)," +
"email VARCHAR(100)," +
"password VARCHAR(100))");

st.executeUpdate("CREATE TABLE IF NOT EXISTS queues(" +
"token_id INT AUTO_INCREMENT PRIMARY KEY," +
"user_id INT," +
"dept_type VARCHAR(50)," +
"dept_name VARCHAR(100)," +
"slot_time VARCHAR(50)," +
"token_number INT)");

}catch(Exception e){
e.printStackTrace();
}

}

static void createUI(){

JFrame frame = new JFrame("QueLess India");
frame.setSize(900,600);
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

mainPanel.add(loginPanel(),"login");
mainPanel.add(signupPanel(),"signup");
mainPanel.add(bookingPanel(),"booking");

frame.add(mainPanel);

card.show(mainPanel,"login");

frame.setVisible(true);

}

static JPanel loginPanel(){

JPanel bg = new JPanel(new GridBagLayout());
bg.setBackground(new Color(240,244,248));

JPanel cardPanel = new JPanel(new GridBagLayout());
cardPanel.setPreferredSize(new Dimension(350,320));
cardPanel.setBackground(Color.white);

GridBagConstraints c = new GridBagConstraints();
c.insets = new Insets(10,10,10,10);

JLabel title = new JLabel("QueLess India");
title.setFont(new Font("Segoe UI",Font.BOLD,24));

JTextField email = new JTextField(18);
email.setPreferredSize(new Dimension(200,35));
addPlaceholder(email,"Email");

JPasswordField pass = new JPasswordField(18);
pass.setPreferredSize(new Dimension(200,35));
addPasswordPlaceholder(pass,"Password");

JButton login = new JButton("Login");
login.setBackground(new Color(33,150,243));
login.setForeground(Color.white);

JButton signup = new JButton("Signup");

c.gridx=0;c.gridy=0;
cardPanel.add(title,c);

c.gridy=1;
cardPanel.add(email,c);

c.gridy=2;
cardPanel.add(pass,c);

c.gridy=3;
cardPanel.add(login,c);

c.gridy=4;
cardPanel.add(signup,c);

bg.add(cardPanel);

login.addActionListener(e -> {

try{

PreparedStatement ps = con.prepareStatement(
"select * from users where email=? and password=?"
);

ps.setString(1,email.getText());
ps.setString(2,new String(pass.getPassword()));

ResultSet rs = ps.executeQuery();

if(rs.next()){

userId = rs.getInt("user_id");
card.show(mainPanel,"booking");

}else{

JOptionPane.showMessageDialog(null,"Invalid Login");

}

}catch(Exception ex){
ex.printStackTrace();
}
});
signup.addActionListener(e -> card.show(mainPanel,"signup"));
return bg;
}
static JPanel signupPanel(){

JPanel bg = new JPanel(new GridBagLayout());
bg.setBackground(new Color(240,244,248));

JPanel cardPanel = new JPanel(new GridBagLayout());
cardPanel.setPreferredSize(new Dimension(350,350));
cardPanel.setBackground(Color.white);

GridBagConstraints c = new GridBagConstraints();
c.insets = new Insets(10,10,10,10);

JLabel title = new JLabel("Create Account");
title.setFont(new Font("Segoe UI",Font.BOLD,24));

JTextField name = new JTextField(18);
addPlaceholder(name,"Full Name");

JTextField email = new JTextField(18);
addPlaceholder(email,"Email");

JPasswordField pass = new JPasswordField(18);
addPasswordPlaceholder(pass,"Password");

JButton signup = new JButton("Signup");
signup.setBackground(new Color(76,175,80));
signup.setForeground(Color.white);

JButton back = new JButton("Back");

c.gridx=0;c.gridy=0;
cardPanel.add(title,c);

c.gridy=1;
cardPanel.add(name,c);

c.gridy=2;
cardPanel.add(email,c);

c.gridy=3;
cardPanel.add(pass,c);

c.gridy=4;
cardPanel.add(signup,c);

c.gridy=5;
cardPanel.add(back,c);

bg.add(cardPanel);

signup.addActionListener(e -> {

try{

PreparedStatement ps = con.prepareStatement(
"insert into users(name,email,password) values(?,?,?)"
);

ps.setString(1,name.getText());
ps.setString(2,email.getText());
ps.setString(3,new String(pass.getPassword()));

ps.executeUpdate();

JOptionPane.showMessageDialog(null,"Account Created");

card.show(mainPanel,"login");

}catch(Exception ex){
ex.printStackTrace();
}

});

back.addActionListener(e -> card.show(mainPanel,"login"));

return bg;

}

static JPanel bookingPanel(){

JPanel bg = new JPanel(new GridBagLayout());
bg.setBackground(new Color(240,244,248));

JPanel form = new JPanel(new GridBagLayout());
form.setPreferredSize(new Dimension(500,380));
form.setBackground(Color.white);

GridBagConstraints c = new GridBagConstraints();
c.insets = new Insets(10,10,10,10);

JLabel title = new JLabel("Book Queue Token");
title.setFont(new Font("Segoe UI",Font.BOLD,22));

ButtonGroup group = new ButtonGroup();

JRadioButton hospital = new JRadioButton("Hospital");
JRadioButton bank = new JRadioButton("Bank");
JRadioButton post = new JRadioButton("Post Office");
JRadioButton other = new JRadioButton("Other");

group.add(hospital);
group.add(bank);
group.add(post);
group.add(other);

JPanel typePanel = new JPanel();
typePanel.add(hospital);
typePanel.add(bank);
typePanel.add(post);
typePanel.add(other);

JTextField dept = new JTextField(18);
addPlaceholder(dept,"Name");

String[] time = {
"9:00 AM","10:00 AM","11:00 AM",
"12:00 PM","1:00 PM","2:00 PM"
};

JComboBox<String> slot = new JComboBox<>(time);

JButton generate = new JButton("Generate Token");
generate.setBackground(new Color(33,150,243));
generate.setForeground(Color.white);

JLabel tokenLabel = new JLabel("");
JLabel waitLabel = new JLabel("");

c.gridx=0;c.gridy=0;
form.add(title,c);

c.gridy=1;
form.add(typePanel,c);

c.gridy=2;
form.add(dept,c);

c.gridy=3;
form.add(slot,c);

c.gridy=4;
form.add(generate,c);

c.gridy=5;
form.add(tokenLabel,c);

c.gridy=6;
form.add(waitLabel,c);

bg.add(form);

generate.addActionListener(e -> {

try{

String type="";

if(hospital.isSelected()) type="Hospital";
if(bank.isSelected()) type="Bank";
if(post.isSelected()) type="Post Office";
if(other.isSelected()) type="Other";

String deptName = dept.getText();
String timeSlot = slot.getSelectedItem().toString();

Statement st = con.createStatement();

ResultSet rs = st.executeQuery(
"select max(token_number) from queues where dept_name='"+deptName+"'"
);

int token = 1;

if(rs.next()) token = rs.getInt(1)+1;

PreparedStatement ps = con.prepareStatement(
"insert into queues(user_id,dept_type,dept_name,slot_time,token_number) values(?,?,?,?,?)"
);

ps.setInt(1,userId);
ps.setString(2,type);
ps.setString(3,deptName);
ps.setString(4,timeSlot);
ps.setInt(5,token);

ps.executeUpdate();

tokenLabel.setText("Token Number : "+token);

int wait = token*4;

waitLabel.setText("Estimated Wait : "+wait+" minutes");

}catch(Exception ex){
ex.printStackTrace();
}

});

return bg;

}

static void addPlaceholder(JTextField field,String text){

field.setText(text);
field.setForeground(Color.GRAY);

field.addFocusListener(new FocusAdapter(){

public void focusGained(FocusEvent e){
if(field.getText().equals(text)){
field.setText("");
field.setForeground(Color.BLACK);
}
}

public void focusLost(FocusEvent e){
if(field.getText().isEmpty()){
field.setText(text);
field.setForeground(Color.GRAY);
}
}

});

}

static void addPasswordPlaceholder(JPasswordField field,String text){

field.setText(text);
field.setEchoChar((char)0);
field.setForeground(Color.GRAY);

field.addFocusListener(new FocusAdapter(){

public void focusGained(FocusEvent e){
if(new String(field.getPassword()).equals(text)){
field.setText("");
field.setEchoChar('•');
field.setForeground(Color.BLACK);
}
}

public void focusLost(FocusEvent e){
if(new String(field.getPassword()).isEmpty()){
field.setText(text);
field.setEchoChar((char)0);
field.setForeground(Color.GRAY);
}
}

});

}

}