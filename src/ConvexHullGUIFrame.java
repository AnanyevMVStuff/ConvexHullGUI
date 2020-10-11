import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;


public class ConvexHullGUIFrame extends JFrame {
    private final int DELAY = 750;
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int pointLimit = 2000;
    private int numberOfPoints = 0;
    private int numberOfPointsInConvexHull = 0;
    private boolean flag = false;

    private final JButton ConvexHullButton;
    private final JButton clearButton;
    private JPanel panel;
    private JLabel label;
    private JLabel label2;

    private ArrayList<Point2D> point2Dlist;
    private Deque<Point2D> convexHulldeque;

    public ConvexHullGUIFrame(){
        super("Convex Hull GUI");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(WIDTH, HEIGHT);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setLayout(new FlowLayout());

        ConvexHullButton = new JButton("Find a convex hull");
        ConvexHullButton.setEnabled(false);
        // Поиск выпуклой оболочки
        ConvexHullButton.addActionListener(e -> {
            convexHulldeque.clear();
            ConvexHullButton.setEnabled(false);
            flag = true;
            int minimumIndex = findMinYPoint();
            Point2D minimumPoint = point2Dlist.get(minimumIndex);
            convexHulldeque.push(minimumPoint);
            panel.repaint();
            ArrayList<Point2D> copy = new ArrayList<>(point2Dlist);
            copy.set(minimumIndex, copy.get(copy.size()-1));
            copy.remove(copy.size()-1);
            copy.sort((p1, p2) -> {
                double angle1 = polarAngle(minimumPoint, p1);
                double angle2 = polarAngle(minimumPoint, p2);
                if (Double.compare(angle1, angle2) != 0){
                    return Double.compare(angle1, angle2);
                } else {
                    // если углы равны, сортируем по расстоянию от p0 (можно и по квадрату расстояния)
                    int x1 = (int)p1.getX();
                    int y1 = (int)p1.getY();
                    int x2 = (int)p2.getX();
                    int y2 = (int)p2.getY();
                    double distSq1 = (x1-minimumPoint.getX())*(x1-minimumPoint.getX()) + (y1-minimumPoint.getY())*(y1-minimumPoint.getY());
                    double distSq2 = (x2-minimumPoint.getX())*(x2-minimumPoint.getX()) + (y2-minimumPoint.getY())*(y2-minimumPoint.getY());
                    if (distSq1 < distSq2){
                        return -1;
                    } else if (distSq1 > distSq2) {
                        return 1;
                    } else {
                        return 0;
                    }
                }

            });
            convexHulldeque.push(copy.get(0));
            panel.repaint();
            final Timer t = new Timer(DELAY, null);
            t.addActionListener(new ActionListener() {
                int counter = 2;
                Point2D prevPoint = copy.get(0);
                Point2D currPoint = copy.get(1);
                Point2D nextPoint;
                {
                    numberOfPointsInConvexHull = 2;
                    label2.setText("Number of points in the convex hull: " + numberOfPointsInConvexHull);
                    if (copy.size() == 2) {
                        nextPoint = minimumPoint;
                    } else {
                        nextPoint = copy.get(2);
                    }
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (counter == 2) {
                        convexHulldeque.push(currPoint);
                        counter++;
                        panel.repaint();
                        numberOfPointsInConvexHull++;
                        label2.setText("Number of points in the convex hull: " + numberOfPointsInConvexHull);
                        return;
                    }
                    if (point2Dlist.size() == 0) {
                        t.stop();
                        flag = false;
                        return;
                    }
                    if (counter - 1 == copy.size()) {
                        t.stop();
                        flag = false;
                        if (point2Dlist.size() > 2)
                            ConvexHullButton.setEnabled(true);
                        convexHulldeque.push(minimumPoint);
                        panel.repaint();
                        return;
                    }
                    int orientationResult = orientation(prevPoint, currPoint, nextPoint);
                    if (orientationResult < 0) {
                        convexHulldeque.push(nextPoint);
                        prevPoint = currPoint;
                        currPoint = nextPoint;
                        numberOfPointsInConvexHull++;
                    } else if (orientationResult == 0){
                        convexHulldeque.pop();
                        prevPoint = convexHulldeque.getFirst();
                        currPoint = nextPoint;
                        convexHulldeque.push(nextPoint);
                    }
                    else {
                        convexHulldeque.pop();
                        currPoint = prevPoint;
                        convexHulldeque.pop();
                        prevPoint = convexHulldeque.getFirst();
                        convexHulldeque.push(currPoint);
                        counter--;
                        numberOfPointsInConvexHull--;
                    }
                    panel.repaint();
                    if (counter != copy.size() && orientationResult <= 0) {
                        nextPoint = copy.get(counter);
                    }
                    label2.setText("Number of points in the convex hull: " + numberOfPointsInConvexHull);
                    counter++;
                }
            });
            t.start();
        });
        clearButton = new JButton("Remove all points");
        // Очистка
        clearButton.addActionListener(e -> {
            point2Dlist.clear();
            convexHulldeque.clear();
            numberOfPoints = 0;
            label.setText("Number of points: " + numberOfPoints);
            numberOfPointsInConvexHull = 0;
            label2.setText("Number of points in the convex hull: " + numberOfPointsInConvexHull);
            panel.repaint();
            ConvexHullButton.setEnabled(false);
        });

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D graphics2D = (Graphics2D) g;
                for (Point2D p : point2Dlist) {
                    Ellipse2D.Double circle = new Ellipse2D.Double(p.getX()-2,p.getY()-2,5, 5);
                    graphics2D.setColor(Color.BLACK);
                    graphics2D.fill(circle);
                }
                boolean flag = false;
                Point2D previousPoint = null;
                for (Point2D p : convexHulldeque) {
                    Ellipse2D.Double circle = new Ellipse2D.Double(p.getX()-2.5,p.getY()-2.5,8, 8);
                    graphics2D.setColor(Color.RED);
                    graphics2D.fill(circle);
                    if (convexHulldeque.size() > 1 && flag) {
                        Line2D line2D = new Line2D.Double(p.getX()+1.5,p.getY()+1.5,previousPoint.getX()+1.5,previousPoint.getY()+1.5);
                        graphics2D.setColor(Color.RED);
                        graphics2D.draw(line2D);
                        previousPoint = p;
                    } else {
                        flag = true;
                        previousPoint = p;
                    }
                }
            }
        };
        panel.setPreferredSize(new Dimension((int)(WIDTH*0.95),(int)(HEIGHT*0.85)));
        panel.setBackground(Color.WHITE);
        panel.addMouseListener(new MouseAdapter() {
            // Добавить точку
            @Override
            public void mousePressed(MouseEvent e) {
                if (numberOfPoints == pointLimit){
                    JOptionPane.showMessageDialog(panel, "There are too many points. Clear the area.");
                    return;
                }
                if (flag) {
                    return;
                }
                if (checkDuplicates(e.getX(), e.getY())) {
                    point2Dlist.add(new Point2D.Double(e.getX(), e.getY()));
                    numberOfPoints++;
                    label.setText("Number of points: " + numberOfPoints);
                    if (numberOfPoints > 2) {
                        ConvexHullButton.setEnabled(true);
                    }
                    panel.repaint();
                }
            }
        });

        label = new JLabel("Number of points: " + numberOfPoints);
        label2 = new JLabel("Number of points in the convex hull: " + numberOfPointsInConvexHull);

        point2Dlist = new ArrayList<>();
        convexHulldeque = new ArrayDeque<>();

        this.add(panel);
        this.add(label);
        this.add(clearButton);
        this.add(ConvexHullButton);
        this.add(label2);

        this.setVisible(true);
    }

    private boolean checkDuplicates(double x, double y){
        for (Point2D point2D : point2Dlist) {
            if ((int) point2D.getX() == (int) x && (int) point2D.getY() == (int) y) {
                return false;
            }
        }
        return true;
    }

    // MinY здесь MaxY, координаты (0,0) в левом верхнем углу панели
    private int findMinYPoint() {
        int N = point2Dlist.size();
        int minimumIndex = 0;
        for (int i = 0; i < N; i++) {
//            System.out.println("Точка " + i);
//            System.out.println("X: " + (int)point2Dlist.get(i).getX() +" Y: " + (int)point2Dlist.get(i).getY());
            if ((int)point2Dlist.get(i).getY() > (int)point2Dlist.get(minimumIndex).getY()) {
                minimumIndex = i;
            } else if((int)point2Dlist.get(i).getY() == (int)point2Dlist.get(minimumIndex).getY()) {
                if ((int)point2Dlist.get(i).getX() < (int)point2Dlist.get(minimumIndex).getX()) {
                    minimumIndex = i;
                }
            }
        }
        return minimumIndex;
    }

    private double polarAngle(Point2D p0, Point2D p1) {
        int x1 = (int)p0.getX();
        int y1 = HEIGHT - (int)p0.getY();
        int x2 = (int)p1.getX();
        int y2 = HEIGHT - (int)p1.getY();
        if (x1 == x2 && y1 == y2) {
            return 0;
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dy < 0 || dx < 0) {
            return  Math.PI + Math.atan(dy/dx);
        }
        return Math.atan(dy/dx);
    }

    private int orientation(Point2D prevPoint, Point2D currPoint, Point2D nextPoint) {
        int x1 = (int)prevPoint.getX();
        int y1 = (int)prevPoint.getY();
        int x2 = (int)currPoint.getX();
        int y2 = (int)currPoint.getY();
        int x3 = (int)nextPoint.getX();
        int y3 = (int)nextPoint.getY();
        return (x2-x1)*(y3-y1) - (y2-y1)*(x3-x1);
    }
}
