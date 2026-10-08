package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.WorkOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkOrderRepository {

    public void save(WorkOrder workOrder) throws SQLException {
        if (workOrder.getId() == 0 || findById(workOrder.getId()) == null) {
            insert(workOrder);
        } else {
            update(workOrder);
        }
        saveServiceItems(workOrder);
    }

    public List<WorkOrder> findAll() throws SQLException {
        List<WorkOrder> workOrders = new ArrayList<WorkOrder>();
        String sql = "SELECT id, booking_id, mechanic_id, status, type, vehicle_id, description, original_work_order_id, planned_date, customer_instructions, other_comments FROM work_orders";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                WorkOrder workOrder = buildWorkOrder(resultSet);
                loadServiceItems(workOrder);
                workOrders.add(workOrder);
            }
        }

        return workOrders;
    }

    public WorkOrder findById(int id) throws SQLException {
        String sql = "SELECT id, booking_id, mechanic_id, status, type, vehicle_id, description, original_work_order_id, planned_date, customer_instructions, other_comments FROM work_orders WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    WorkOrder workOrder = buildWorkOrder(resultSet);
                    loadServiceItems(workOrder);
                    return workOrder;
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String deleteLinks = "DELETE FROM work_order_service_items WHERE work_order_id = ?";
        String deleteOrder = "DELETE FROM work_orders WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement links = connection.prepareStatement(deleteLinks);
             PreparedStatement order = connection.prepareStatement(deleteOrder)) {

            links.setInt(1, id);
            links.executeUpdate();

            order.setInt(1, id);
            order.executeUpdate();
        }
    }

    private void insert(WorkOrder workOrder) throws SQLException {
        String sql = "INSERT INTO work_orders (booking_id, mechanic_id, status, type, vehicle_id, description, original_work_order_id, planned_date, customer_instructions, other_comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, workOrder.getBookingId());
            statement.setInt(2, workOrder.getMechanicId());
            statement.setString(3, workOrder.getStatus());
            statement.setString(4, workOrder.getType());
            statement.setInt(5, workOrder.getVehicleId());
            statement.setString(6, workOrder.getDescription());
            statement.setInt(7, workOrder.getOriginalWorkOrderId());
            statement.setString(8, workOrder.getPlannedDate());
            statement.setString(9, workOrder.getCustomerInstructions());
            statement.setString(10, workOrder.getOtherComments());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    workOrder.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(WorkOrder workOrder) throws SQLException {
        String sql = "UPDATE work_orders SET booking_id = ?, mechanic_id = ?, status = ?, type = ?, vehicle_id = ?, description = ?, original_work_order_id = ?, planned_date = ?, customer_instructions = ?, other_comments = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, workOrder.getBookingId());
            statement.setInt(2, workOrder.getMechanicId());
            statement.setString(3, workOrder.getStatus());
            statement.setString(4, workOrder.getType());
            statement.setInt(5, workOrder.getVehicleId());
            statement.setString(6, workOrder.getDescription());
            statement.setInt(7, workOrder.getOriginalWorkOrderId());
            statement.setString(8, workOrder.getPlannedDate());
            statement.setString(9, workOrder.getCustomerInstructions());
            statement.setString(10, workOrder.getOtherComments());
            statement.setInt(11, workOrder.getId());
            statement.executeUpdate();
        }
    }

    private void saveServiceItems(WorkOrder workOrder) throws SQLException {
        String deleteLinks = "DELETE FROM work_order_service_items WHERE work_order_id = ?";
        String insertLink = "INSERT INTO work_order_service_items (work_order_id, service_item_id, completed, price) VALUES (?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement delete = connection.prepareStatement(deleteLinks);
             PreparedStatement insert = connection.prepareStatement(insertLink)) {

            delete.setInt(1, workOrder.getId());
            delete.executeUpdate();

            for (Integer serviceItemId : workOrder.getServiceItemIds()) {
                int isCompleted = workOrder.getCompletedServiceItems().contains(serviceItemId) ? 1 : 0;
                insert.setInt(1, workOrder.getId());
                insert.setInt(2, serviceItemId);
                insert.setInt(3, isCompleted);

                // Spara priset som gällde när arbetet utfördes.
                Double frozenPrice = workOrder.getCompletedServicePrice(serviceItemId);
                if (frozenPrice == null) {
                    insert.setNull(4, Types.REAL);
                } else {
                    insert.setDouble(4, frozenPrice.doubleValue());
                }
                insert.executeUpdate();
            }
        }
    }

    private void loadServiceItems(WorkOrder workOrder) throws SQLException {
        String sql = "SELECT service_item_id, completed, price FROM work_order_service_items WHERE work_order_id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, workOrder.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Integer> services = new ArrayList<Integer>();
                List<Integer> completed = new ArrayList<Integer>();
                Map<Integer, Double> prices = new LinkedHashMap<Integer, Double>();
                while (resultSet.next()) {
                    int sid = resultSet.getInt("service_item_id");
                    int comp = resultSet.getInt("completed");
                    services.add(sid);
                    if (comp == 1) {
                        completed.add(sid);
                    }
                    double price = resultSet.getDouble("price");
                    if (!resultSet.wasNull()) {
                        prices.put(Integer.valueOf(sid), Double.valueOf(price));
                    }
                }
                workOrder.setServiceItemIds(services);
                workOrder.setCompletedServiceItems(completed);
                workOrder.setCompletedServicePrices(prices);
            }
        }
    }


    private WorkOrder buildWorkOrder(ResultSet resultSet) throws SQLException {
        WorkOrder workOrder = new WorkOrder(
                resultSet.getInt("id"),
                resultSet.getInt("booking_id"),
                resultSet.getInt("mechanic_id")
        );

        String status = resultSet.getString("status");
        String type = resultSet.getString("type");

        workOrder.setVehicleId(resultSet.getInt("vehicle_id"));
        workOrder.setDescription(resultSet.getString("description"));

        if (status != null) {
            workOrder.setStatus(status);
        }
        // En rad utan typ (äldre eller halvfärdig) behåller modellens standard i stället för tomt.
        if (type != null && !type.trim().isEmpty()) {
            workOrder.setType(type);
        }

        workOrder.setOriginalWorkOrderId(resultSet.getInt("original_work_order_id"));
        workOrder.setPlannedDate(resultSet.getString("planned_date"));
        workOrder.setCustomerInstructions(resultSet.getString("customer_instructions"));
        workOrder.setOtherComments(resultSet.getString("other_comments"));

        return workOrder;
    }
}
