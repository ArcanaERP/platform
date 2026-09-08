package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPartyDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyRoleTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyRoleTypeView;
import com.arcanaerp.platform.inventory.InventoryPartyView;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyCommand;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyRoleTypeCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryPartyReferenceDataController {

    private final InventoryPartyDirectory partyDirectory;
    private final InventoryPartyRoleTypeDirectory roleTypeDirectory;

    @PostMapping("/parties")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryPartyResponse createParty(@Valid @RequestBody CreateInventoryPartyRequest request) {
        return toResponse(partyDirectory.registerParty(
            new RegisterInventoryPartyCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/parties/{code}")
    public InventoryPartyResponse partyByCode(@PathVariable String code) {
        return toResponse(partyDirectory.partyByCode(code));
    }

    @GetMapping("/parties")
    public PageResult<InventoryPartyResponse> listParties(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return partyDirectory.listParties(PageQuery.of(page, size)).map(this::toResponse);
    }

    @PostMapping("/party-role-types")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryPartyRoleTypeResponse createRoleType(
        @Valid @RequestBody CreateInventoryPartyRoleTypeRequest request
    ) {
        return toResponse(roleTypeDirectory.registerRoleType(
            new RegisterInventoryPartyRoleTypeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/party-role-types/{code}")
    public InventoryPartyRoleTypeResponse roleTypeByCode(@PathVariable String code) {
        return toResponse(roleTypeDirectory.roleTypeByCode(code));
    }

    @GetMapping("/party-role-types")
    public PageResult<InventoryPartyRoleTypeResponse> listRoleTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return roleTypeDirectory.listRoleTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryPartyResponse toResponse(InventoryPartyView party) {
        return new InventoryPartyResponse(
            party.id(),
            party.code(),
            party.description(),
            party.createdAt()
        );
    }

    private InventoryPartyRoleTypeResponse toResponse(InventoryPartyRoleTypeView type) {
        return new InventoryPartyRoleTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.createdAt()
        );
    }
}
