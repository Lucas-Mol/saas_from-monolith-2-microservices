package dev.agenda.gateway.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.agenda.gateway.migration.MigrationTableView;
import dev.agenda.gateway.migration.RoutingTable;

@RestController
public class MigrationAdminController {

    private final RoutingTable routingTable;

    public MigrationAdminController(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }

    @GetMapping("/admin/migration")
    public MigrationTableView migration() {
        return routingTable.view();
    }
}
