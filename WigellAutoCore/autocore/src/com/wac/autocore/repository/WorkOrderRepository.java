package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.WorkOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT id, booking_id, mechanic_id, status FROM work_orders";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                WorkOrder workOrder = buildWorkOrder(resultSet);
                workOrder.setServiceItemIds(findServiceItemIds(workOrder.getId()));
                workOrders.add(workOrder);
            }
        }

        return workOrders;
    }

    public WorkOrder findById(int id) throws SQLException {
        String sql = "SELECT id, booking_id, mechanic_id, status FROM work_orders WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    WorkOrder workOrder = buildWorkOrder(resultSet);
                    workOrder.setServiceItemIds(findServiceItemIds(workOrder.getId()));
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
        String sql = "INSERT INTO work_orders (booking_id, mechanic_id, status) VALUES (?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, workOrder.getBookingId());
            statement.setInt(2, workOrder.getMechanicId());
            statement.setString(3, workOrder.getStatus());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    workOrder.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(WorkOrder workOrder) throws SQLException {
        String sql = "UPDATE work_orders SET booking_id = ?, mechanic_id = ?, status = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, workOrder.getBookingId());
            statement.setInt(2, workOrder.getMechanicId());
            statement.setString(3, workOrder.getStatus());
            statement.setInt(4, workOrder.getId());
            statement.executeUpdate();
        }
    }

    private void saveServiceItems(WorkOrder workOrder) throws SQLException {
        String deleteLinks = "DELETE FROM work_order_service_items WHERE work_order_id = ?";
        String insertLink = "INSERT INTO work_order_service_items (work_order_id, service_item_id) VALUES (?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement delete = connection.prepareStatement(deleteLinks);
             PreparedStatement insert = connection.prepareStatement(insertLink)) {

            delete.setInt(1, workOrder.getId());
            delete.executeUpdate();

            for (Integer serviceItemId : workOrder.getServiceItemIds()) {
                insert.setInt(1, workOrder.getId());
                insert.setInt(2, serviceItemId);
                insert.executeUpdate();
            }
        }
    }

    private List<Integer> findServiceItemIds(int workOrderId) throws SQLException {
        List<Integer> serviceItemIds = new ArrayList<Integer>();
        String sql = "SELECT service_item_id FROM work_order_service_items WHERE work_order_id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, workOrderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    serviceItemIds.add(resultSet.getInt("service_item_id"));
                }
            }
        }

        return serviceItemIds;
    }

    private WorkOrder buildWorkOrder(ResultSet resultSet) throws SQLException {
        WorkOrder workOrder = new WorkOrder(
                resultSet.getInt("id"),
                resultSet.getInt("booking_id"),
                resultSet.getInt("mechanic_id")
        );

        String status = resultSet.getString("status");
        if (status != null) {
            workOrder.setStatus(status);
        }

        return workOrder;
    }
}
