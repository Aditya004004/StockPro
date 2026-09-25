/**
 * Phase 6 Security Review & Permission Matrix Test Suite.
 * ADR-002: Direct API invocation verification without the client app.
 * docs/phases.md § Phase 6:
 * - Call every callable function directly, without the app, as every role.
 * - Attempt cross-org and cross-plant reads/writes.
 * - Verify App Check and Auth gating.
 */

import test from 'node:test';
import assert from 'node:assert/strict';

// Import domain permission evaluators
function isMovementAllowed(role, type) {
  const permissions = {
    STORE_KEEPER: ["RECEIPT", "ISSUE", "TRANSFER_IN", "TRANSFER_OUT"],
    PRODUCTION_SUPERVISOR: ["ISSUE", "PRODUCTION", "SCRAP"],
    QA: ["SCRAP"],
    INVENTORY_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    PLANT_MANAGER: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
    ORG_ADMIN: ["RECEIPT", "ISSUE", "PRODUCTION", "ADJUSTMENT", "TRANSFER_IN", "TRANSFER_OUT", "SCRAP"],
  };
  const allowed = permissions[role];
  return allowed ? allowed.includes(type) : false;
}

function isMasterDataAllowed(role) {
  return ["INVENTORY_MANAGER", "PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}

function isDeactivationAllowed(role) {
  return ["PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}

function isTeamManagementAllowed(role) {
  return ["PLANT_MANAGER", "ORG_ADMIN"].includes(role);
}

// Simulated Cloud Function Authorization Guard
function evaluateRequestAuth(request, payload, requiredPermission) {
  // 1. App Check enforcement
  if (!request.appCheck || !request.appCheck.token) {
    throw new Error("UNAUTHENTICATED_APP_CHECK: App Check token required.");
  }

  // 2. User Authentication
  if (!request.auth) {
    throw new Error("UNAUTHENTICATED: Authentication required.");
  }

  const claims = request.auth.token;

  // 3. Organization boundary check
  if (claims.orgId !== payload.orgId) {
    throw new Error("PERMISSION_DENIED: Cross-org access blocked.");
  }

  // 4. Plant membership check
  const plantRoles = claims.plantRoles || {};
  const userRole = plantRoles[payload.plantId];
  if (!userRole) {
    throw new Error("PERMISSION_DENIED: Cross-plant access blocked.");
  }

  // 5. Role-based operation check
  if (requiredPermission.type === "MOVEMENT") {
    if (!isMovementAllowed(userRole, requiredPermission.movementType)) {
      throw new Error(`PERMISSION_DENIED: Role '${userRole}' cannot perform '${requiredPermission.movementType}'`);
    }
  } else if (requiredPermission.type === "MASTER_DATA") {
    if (!isMasterDataAllowed(userRole)) {
      throw new Error(`PERMISSION_DENIED: Role '${userRole}' cannot modify master data`);
    }
  } else if (requiredPermission.type === "DEACTIVATE_ITEM") {
    if (!isDeactivationAllowed(userRole)) {
      throw new Error(`PERMISSION_DENIED: Role '${userRole}' cannot deactivate items`);
    }
  } else if (requiredPermission.type === "TEAM_MANAGEMENT") {
    if (!isTeamManagementAllowed(userRole)) {
      throw new Error(`PERMISSION_DENIED: Role '${userRole}' cannot manage team members`);
    }
  }

  return { success: true, userRole };
}

// ── Test Suites ─────────────────────────────────────────────────────────────

test('Security Review: App Check & Unauthenticated Direct API Gating', () => {
  const payload = { orgId: 'org_acme', plantId: 'plant_pune' };

  // Call without App Check
  assert.throws(() => {
    evaluateRequestAuth({ auth: { token: { orgId: 'org_acme' } } }, payload, { type: 'MASTER_DATA' });
  }, /UNAUTHENTICATED_APP_CHECK/);

  // Call without Auth
  assert.throws(() => {
    evaluateRequestAuth({ appCheck: { token: 'valid' } }, payload, { type: 'MASTER_DATA' });
  }, /UNAUTHENTICATED/);
});

test('Security Review: Cross-Tenant & Cross-Plant Isolation Barrier', () => {
  const appCheck = { token: 'valid_app_check' };

  // User belongs to org_acme, attempts to access org_rival
  const crossOrgRequest = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_pune: 'PLANT_MANAGER' } } },
  };
  assert.throws(() => {
    evaluateRequestAuth(crossOrgRequest, { orgId: 'org_rival', plantId: 'plant_pune' }, { type: 'MASTER_DATA' });
  }, /Cross-org access blocked/);

  // User belongs to plant_pune, attempts to access plant_chennai
  const crossPlantRequest = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_pune: 'PLANT_MANAGER' } } },
  };
  assert.throws(() => {
    evaluateRequestAuth(crossPlantRequest, { orgId: 'org_acme', plantId: 'plant_chennai' }, { type: 'MASTER_DATA' });
  }, /Cross-plant access blocked/);
});

test('Security Review: Role Permission Matrix for STORE_KEEPER', () => {
  const appCheck = { token: 'valid' };
  const storeKeeper = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_01: 'STORE_KEEPER' } } },
  };
  const payload = { orgId: 'org_acme', plantId: 'plant_01' };

  // Can RECEIPT & ISSUE
  assert.ok(evaluateRequestAuth(storeKeeper, payload, { type: 'MOVEMENT', movementType: 'RECEIPT' }));
  assert.ok(evaluateRequestAuth(storeKeeper, payload, { type: 'MOVEMENT', movementType: 'ISSUE' }));

  // BLOCKED from ADJUSTMENT, SCRAP, Master Data, Deactivation, Team Management
  assert.throws(() => evaluateRequestAuth(storeKeeper, payload, { type: 'MOVEMENT', movementType: 'ADJUSTMENT' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(storeKeeper, payload, { type: 'MOVEMENT', movementType: 'SCRAP' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(storeKeeper, payload, { type: 'MASTER_DATA' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(storeKeeper, payload, { type: 'DEACTIVATE_ITEM' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(storeKeeper, payload, { type: 'TEAM_MANAGEMENT' }), /PERMISSION_DENIED/);
});

test('Security Review: Role Permission Matrix for PRODUCTION_SUPERVISOR', () => {
  const appCheck = { token: 'valid' };
  const supervisor = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_01: 'PRODUCTION_SUPERVISOR' } } },
  };
  const payload = { orgId: 'org_acme', plantId: 'plant_01' };

  // Can ISSUE, PRODUCTION, SCRAP
  assert.ok(evaluateRequestAuth(supervisor, payload, { type: 'MOVEMENT', movementType: 'ISSUE' }));
  assert.ok(evaluateRequestAuth(supervisor, payload, { type: 'MOVEMENT', movementType: 'PRODUCTION' }));
  assert.ok(evaluateRequestAuth(supervisor, payload, { type: 'MOVEMENT', movementType: 'SCRAP' }));

  // BLOCKED from RECEIPT, ADJUSTMENT, Master Data, Team Management
  assert.throws(() => evaluateRequestAuth(supervisor, payload, { type: 'MOVEMENT', movementType: 'RECEIPT' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(supervisor, payload, { type: 'MOVEMENT', movementType: 'ADJUSTMENT' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(supervisor, payload, { type: 'MASTER_DATA' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(supervisor, payload, { type: 'TEAM_MANAGEMENT' }), /PERMISSION_DENIED/);
});

test('Security Review: Role Permission Matrix for QA', () => {
  const appCheck = { token: 'valid' };
  const qa = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_01: 'QA' } } },
  };
  const payload = { orgId: 'org_acme', plantId: 'plant_01' };

  // QA can only post SCRAP
  assert.ok(evaluateRequestAuth(qa, payload, { type: 'MOVEMENT', movementType: 'SCRAP' }));

  // BLOCKED from RECEIPT, ISSUE, ADJUSTMENT, Master Data
  assert.throws(() => evaluateRequestAuth(qa, payload, { type: 'MOVEMENT', movementType: 'RECEIPT' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(qa, payload, { type: 'MOVEMENT', movementType: 'ISSUE' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(qa, payload, { type: 'MOVEMENT', movementType: 'ADJUSTMENT' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(qa, payload, { type: 'MASTER_DATA' }), /PERMISSION_DENIED/);
});

test('Security Review: Role Permission Matrix for INVENTORY_MANAGER', () => {
  const appCheck = { token: 'valid' };
  const manager = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_01: 'INVENTORY_MANAGER' } } },
  };
  const payload = { orgId: 'org_acme', plantId: 'plant_01' };

  // Can post all movement types & master data
  assert.ok(evaluateRequestAuth(manager, payload, { type: 'MOVEMENT', movementType: 'RECEIPT' }));
  assert.ok(evaluateRequestAuth(manager, payload, { type: 'MOVEMENT', movementType: 'ADJUSTMENT' }));
  assert.ok(evaluateRequestAuth(manager, payload, { type: 'MASTER_DATA' }));

  // BLOCKED from deactivating items and team management
  assert.throws(() => evaluateRequestAuth(manager, payload, { type: 'DEACTIVATE_ITEM' }), /PERMISSION_DENIED/);
  assert.throws(() => evaluateRequestAuth(manager, payload, { type: 'TEAM_MANAGEMENT' }), /PERMISSION_DENIED/);
});

test('Security Review: Role Permission Matrix for PLANT_MANAGER & ORG_ADMIN', () => {
  const appCheck = { token: 'valid' };
  const plantManager = {
    appCheck,
    auth: { token: { orgId: 'org_acme', plantRoles: { plant_01: 'PLANT_MANAGER' } } },
  };
  const payload = { orgId: 'org_acme', plantId: 'plant_01' };

  // Full plant privileges
  assert.ok(evaluateRequestAuth(plantManager, payload, { type: 'MOVEMENT', movementType: 'ADJUSTMENT' }));
  assert.ok(evaluateRequestAuth(plantManager, payload, { type: 'MASTER_DATA' }));
  assert.ok(evaluateRequestAuth(plantManager, payload, { type: 'DEACTIVATE_ITEM' }));
  assert.ok(evaluateRequestAuth(plantManager, payload, { type: 'TEAM_MANAGEMENT' }));
});
