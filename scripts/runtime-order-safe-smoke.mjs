import { execFileSync } from "node:child_process";

const adminBase = process.env.CRMEB_ADMIN_WEB || "http://127.0.0.1:19527";
const account = process.env.CRMEB_ADMIN_ACCOUNT || "admin";
const pwd = process.env.CRMEB_ADMIN_PWD || "123456";
const timeoutMs = Number(process.env.CRMEB_RUNTIME_CHECK_TIMEOUT || 10000);
const mysqlBin = process.env.CRMEB_MYSQL_BIN
  || "/Users/chenwankang/Documents/jsy/聚商盈一体化电商系统/crmeb_源码/.local-runtime/mysql/bin/mysql";
const mysqlSocket = process.env.CRMEB_MYSQL_SOCKET || "/tmp/crmeb-copy-mysql.sock";
const mysqlDatabase = process.env.CRMEB_MYSQL_DATABASE || "single_open";

async function requestJson(url, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...options, signal: controller.signal });
    const text = await response.text();
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      body = { raw: text.slice(0, 200) };
    }
    return { response, body };
  } finally {
    clearTimeout(timer);
  }
}

function assertCode(name, result, code) {
  if (result.body?.code !== code) {
    throw new Error(`${name}: expected code ${code}, got ${JSON.stringify({
      httpStatus: result.response.status,
      code: result.body?.code,
      message: result.body?.message,
      raw: result.body?.raw
    })}`);
  }
}

function assertOk(name, result) {
  assertCode(name, result, 200);
  return result.body.data;
}

function assertTruthy(name, value, context) {
  if (!value) {
    throw new Error(`${name}: assertion failed ${JSON.stringify(context)}`);
  }
}

function listRows(data) {
  return Array.isArray(data?.list) ? data.list : [];
}

function firstRow(data) {
  return listRows(data)[0] || null;
}

function orderNo(row) {
  return row?.orderId || row?.orderNo || "";
}

async function get(name, url, token) {
  return assertOk(name, await requestJson(url, {
    headers: token ? { "Authori-zation": token } : {}
  }));
}

async function post(name, url, body, token) {
  return assertOk(name, await requestJson(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { "Authori-zation": token } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  }));
}

function assertFields(name, data, fields) {
  const missing = fields.filter((field) => data?.[field] === undefined);
  if (missing.length) {
    throw new Error(`${name} missing fields: ${missing.join(", ")}`);
  }
}

function normalizeImages(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  const text = String(value).trim();
  if (!text) return [];
  if (text.startsWith("[")) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean);
    } catch {
      return [];
    }
  }
  return text.split(",").map((item) => item.trim()).filter(Boolean);
}

function assetLooksLocalOrSafe(value) {
  return /^(https?:)?\/\//.test(value) || value.startsWith("/") || value.startsWith("data:") || value.startsWith("blob:");
}

function sqlString(value) {
  if (value === null || value === undefined) return "null";
  return `'${String(value).replace(/\\/g, "\\\\").replace(/'/g, "''")}'`;
}

function runMysql(sql) {
  return execFileSync(mysqlBin, [
    `--socket=${mysqlSocket}`,
    "-uroot",
    mysqlDatabase,
    "-N",
    "-e",
    sql
  ], { encoding: "utf8" }).trim();
}

async function findOrderByStatus(status, token) {
  const data = await get(
    `admin order list ${status}`,
    `${adminBase}/api/admin/store/order/list?page=1&limit=5&status=${encodeURIComponent(status)}&type=2`,
    token
  );
  return firstRow(data);
}

function queryOne(sql) {
  const output = runMysql(sql);
  if (!output) return null;
  const [line] = output.split(/\r?\n/);
  if (!line) return null;
  const [id, orderId, paid, status, refundStatus, payPrice, isAlterPrice] = line.split("\t");
  return {
    id: Number(id),
    orderId,
    paid: Number(paid),
    status: Number(status),
    refundStatus: Number(refundStatus),
    payPrice,
    isAlterPrice: Number(isAlterPrice)
  };
}

function queryOrderDeliveryCandidate() {
  const output = runMysql(`
    select id, order_id, paid, status, refund_status, shipping_type, is_del, is_system_del,
           delivery_type, delivery_name, delivery_id, delivery_code, express_record_type
    from eb_store_order
    where paid = 1
      and refund_status = 0
      and shipping_type = 1
      and is_del = 0
      and is_system_del = 0
    order by status asc, id desc
    limit 1;
  `);
  if (!output) return null;
  const [line] = output.split(/\r?\n/);
  if (!line) return null;
  const [
    id,
    orderId,
    paid,
    status,
    refundStatus,
    shippingType,
    isDel,
    isSystemDel,
    deliveryType,
    deliveryName,
    deliveryId,
    deliveryCode,
    expressRecordType
  ] = line.split("\t");
  return {
    id: Number(id),
    orderId,
    paid: Number(paid),
    status: Number(status),
    refundStatus: Number(refundStatus),
    shippingType: Number(shippingType),
    isDel: Number(isDel),
    isSystemDel: Number(isSystemDel),
    deliveryType: deliveryType === "NULL" ? "" : deliveryType,
    deliveryName: deliveryName === "NULL" ? "" : deliveryName,
    deliveryId: deliveryId === "NULL" ? "" : deliveryId,
    deliveryCode: deliveryCode === "NULL" ? "" : deliveryCode,
    expressRecordType: expressRecordType === "NULL" ? null : Number(expressRecordType)
  };
}

const loginData = assertOk("admin login", await requestJson(`${adminBase}/api/admin/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ account, pwd })
}));
const token = loginData.token;

const unauthorizedChecks = [
  ["order send no token", `${adminBase}/api/admin/store/order/send`, "POST", { orderNo: "missing", deliveryType: "noNeed" }],
  ["order mark no token", `${adminBase}/api/admin/store/order/mark?orderNo=missing&mark=x`, "POST"],
  ["order price no token", `${adminBase}/api/admin/store/order/update/price`, "POST", { orderNo: "missing", payPrice: "1.00" }],
  ["order refund no token", `${adminBase}/api/admin/store/order/refund?orderNo=missing&amount=1`, "GET"],
  ["order refund refuse no token", `${adminBase}/api/admin/store/order/refund/refuse?orderNo=missing&reason=x`, "GET"],
  ["order writeoff no token", `${adminBase}/api/admin/store/order/writeUpdate/missing`, "GET"]
];

const unauthorized = [];
for (const [name, url, method, body] of unauthorizedChecks) {
  const result = await requestJson(url, {
    method,
    headers: body ? { "Content-Type": "application/json" } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  assertCode(name, result, 401);
  unauthorized.push({ name, code: result.body.code });
}

const statusNum = await get("admin order status num", `${adminBase}/api/admin/store/order/status/num?type=2`, token);
for (const field of ["all", "unPaid", "notShipped", "toBeWrittenOff", "refunding", "refunded", "refundRefused", "deleted"]) {
  assertTruthy(`status num ${field}`, statusNum[field] !== undefined, statusNum);
}

const allOrder = await findOrderByStatus("all", token);
assertTruthy("admin order list has data", allOrder, {});
const allOrderNo = orderNo(allOrder);
const detail = await get(
  "admin order detail",
  `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(allOrderNo)}`,
  token
);
assertFields("admin order detail", detail, [
  "id",
  "orderId",
  "productList",
  "statusStr",
  "refundStatus",
  "refundPrice",
  "refundReasonWapImg",
  "refundReasonWapExplain",
  "refundReasonTime",
  "refundReasonWap",
  "refundReason",
  "deliveryType",
  "deliveryName",
  "deliveryId",
  "deliveryCode",
  "expressRecordType",
  "deliveryTime",
  "verifyCode",
  "shippingType"
]);
assertTruthy("admin order detail product list", Array.isArray(detail.productList), detail);
await get(
  "admin order status list",
  `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(allOrderNo)}&page=1&limit=5`,
  token
);

const originalRemark = detail.remark || "";
const markMarker = `runtime-order-mark-${Date.now()}`;
let markRestored = false;
try {
  await post(
    "admin order mark writeback",
    `${adminBase}/api/admin/store/order/mark?${new URLSearchParams({
      orderNo: allOrderNo,
      mark: markMarker
    })}`,
    undefined,
    token
  );
  const afterMark = await get(
    "admin order mark detail readback",
    `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(allOrderNo)}`,
    token
  );
  assertTruthy("admin order mark remark saved", afterMark.remark === markMarker, afterMark);
  const statusAfterMark = await get(
    "admin order mark status readback",
    `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(allOrderNo)}&page=1&limit=20`,
    token
  );
  assertTruthy(
    "admin order mark status log saved",
    listRows(statusAfterMark).some((row) => row.changeType === "mark" && String(row.changeMessage || "").includes(markMarker)),
    statusAfterMark
  );
} finally {
  runMysql(`
    update eb_store_order
    set remark = ${sqlString(originalRemark)}
    where order_id = ${sqlString(allOrderNo)};

    delete s
    from eb_store_order_status s
    inner join eb_store_order o on o.id = s.oid
    where o.order_id = ${sqlString(allOrderNo)}
      and s.change_type = 'mark'
      and s.change_message = ${sqlString(`订单备注：${markMarker}`)};
  `);
  markRestored = true;
}
const afterMarkRestore = await get(
  "admin order mark restored detail",
  `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(allOrderNo)}`,
  token
);
assertTruthy("admin order mark remark restored", (afterMarkRestore.remark || "") === originalRemark, afterMarkRestore);
const markLeftovers = Number(runMysql(`
  select count(1)
  from eb_store_order_status s
  inner join eb_store_order o on o.id = s.oid
  where o.order_id = ${sqlString(allOrderNo)}
    and s.change_type = 'mark'
    and s.change_message like ${sqlString(`%${markMarker}%`)};
`));
assertTruthy("admin order mark status marker cleaned", markLeftovers === 0, { markLeftovers, allOrderNo, markMarker });
const globalMarkLeftovers = Number(runMysql(`
  select count(1)
  from eb_store_order_status
  where change_type = 'mark'
    and change_message like '%runtime-order-mark-%';
`));
assertTruthy("admin order mark global markers cleaned", globalMarkLeftovers === 0, { globalMarkLeftovers });

const unpaidOrder = queryOne(`
  select id, order_id, paid, status, refund_status, pay_price, is_alter_price
  from eb_store_order
  where paid = 0
    and is_system_del = 0
  order by id asc
  limit 1;
`);
let priceWriteback = null;
if (unpaidOrder) {
  const originalPayPrice = unpaidOrder.payPrice;
  const originalIsAlterPrice = unpaidOrder.isAlterPrice;
  const nextPayPrice = (Math.max(0, Number(originalPayPrice || 0) - 0.01)).toFixed(2);
  await post(
    "admin order price writeback",
    `${adminBase}/api/admin/store/order/update/price`,
    {
      orderNo: unpaidOrder.orderId,
      payPrice: nextPayPrice
    },
    token
  );
  const afterPrice = await get(
    "admin order price detail readback",
    `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(unpaidOrder.orderId)}`,
    token
  );
  assertTruthy("admin order price saved", Number(afterPrice.payPrice).toFixed(2) === nextPayPrice, afterPrice);
  assertTruthy("admin order price alter flag saved", afterPrice.isAlterPrice === true, afterPrice);
  const statusAfterPrice = await get(
    "admin order price status readback",
    `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(unpaidOrder.orderId)}&page=1&limit=20`,
    token
  );
  assertTruthy(
    "admin order price status log saved",
    listRows(statusAfterPrice).some((row) => row.changeType === "edit_price" && String(row.changeMessage || "").includes(nextPayPrice)),
    statusAfterPrice
  );
  runMysql(`
    update eb_store_order
    set pay_price = ${sqlString(originalPayPrice)},
        is_alter_price = ${Number(originalIsAlterPrice || 0)}
    where order_id = ${sqlString(unpaidOrder.orderId)};

    delete s
    from eb_store_order_status s
    inner join eb_store_order o on o.id = s.oid
    where o.order_id = ${sqlString(unpaidOrder.orderId)}
      and s.change_type = 'edit_price'
      and s.change_message = ${sqlString(`订单改价为${nextPayPrice}元`)};
  `);
  const afterPriceRestore = await get(
    "admin order price restored detail",
    `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(unpaidOrder.orderId)}`,
    token
  );
  assertTruthy("admin order price restored", Number(afterPriceRestore.payPrice).toFixed(2) === Number(originalPayPrice).toFixed(2), afterPriceRestore);
  assertTruthy("admin order price alter flag restored", (afterPriceRestore.isAlterPrice === true ? 1 : 0) === Number(originalIsAlterPrice || 0), afterPriceRestore);
  const priceLeftovers = Number(runMysql(`
    select count(1)
    from eb_store_order_status s
    inner join eb_store_order o on o.id = s.oid
    where o.order_id = ${sqlString(unpaidOrder.orderId)}
      and s.change_type = 'edit_price'
      and s.change_message = ${sqlString(`订单改价为${nextPayPrice}元`)};
  `));
  assertTruthy("admin order price marker cleaned", priceLeftovers === 0, { priceLeftovers, orderNo: unpaidOrder.orderId });
  priceWriteback = {
    orderNo: unpaidOrder.orderId,
    originalPayPrice,
    nextPayPrice,
    restored: true,
    leftovers: priceLeftovers
  };
} else {
  const paidPriceCandidate = queryOne(`
    select id, order_id, paid, status, refund_status, pay_price, is_alter_price
    from eb_store_order
    where paid = 1
      and status = 0
      and refund_status = 0
      and is_del = 0
      and is_system_del = 0
      and pay_price > 0.02
    order by pay_price desc, id desc
    limit 1;
  `);
  if (paidPriceCandidate) {
    const originalPaid = paidPriceCandidate.paid;
    const originalStatus = paidPriceCandidate.status;
    const originalRefundStatus = paidPriceCandidate.refundStatus;
    const originalPayPrice = paidPriceCandidate.payPrice;
    const originalIsAlterPrice = paidPriceCandidate.isAlterPrice;
    const nextPayPrice = (Math.max(0, Number(originalPayPrice || 0) - 0.01)).toFixed(2);
    try {
      runMysql(`
        update eb_store_order
        set paid = 0,
            status = 0,
            refund_status = 0
        where order_id = ${sqlString(paidPriceCandidate.orderId)};
      `);
      const tempUnpaidOrder = await findOrderByStatus("unPaid", token);
      assertTruthy("temporary unpaid order listed", orderNo(tempUnpaidOrder) === paidPriceCandidate.orderId, tempUnpaidOrder);
      await post(
        "admin temporary order price writeback",
        `${adminBase}/api/admin/store/order/update/price`,
        {
          orderNo: paidPriceCandidate.orderId,
          payPrice: nextPayPrice
        },
        token
      );
      const afterPrice = await get(
        "admin temporary order price detail readback",
        `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(paidPriceCandidate.orderId)}`,
        token
      );
      assertTruthy("temporary order price saved", Number(afterPrice.payPrice).toFixed(2) === nextPayPrice, afterPrice);
      assertTruthy("temporary order price alter flag saved", afterPrice.isAlterPrice === true, afterPrice);
      assertTruthy("temporary order price status key", afterPrice.statusStr?.key === "unPaid", afterPrice.statusStr);
      const statusAfterPrice = await get(
        "admin temporary order price status readback",
        `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(paidPriceCandidate.orderId)}&page=1&limit=20`,
        token
      );
      assertTruthy(
        "temporary order price status log saved",
        listRows(statusAfterPrice).some((row) => row.changeType === "edit_price" && String(row.changeMessage || "").includes(nextPayPrice)),
        statusAfterPrice
      );
    } finally {
      runMysql(`
        update eb_store_order
        set paid = ${Number(originalPaid || 0)},
            status = ${Number(originalStatus || 0)},
            refund_status = ${Number(originalRefundStatus || 0)},
            pay_price = ${sqlString(originalPayPrice)},
            is_alter_price = ${Number(originalIsAlterPrice || 0)}
        where order_id = ${sqlString(paidPriceCandidate.orderId)};

        delete s
        from eb_store_order_status s
        inner join eb_store_order o on o.id = s.oid
        where o.order_id = ${sqlString(paidPriceCandidate.orderId)}
          and s.change_type = 'edit_price'
          and s.change_message = ${sqlString(`订单改价为${nextPayPrice}元`)};
      `);
    }
    const afterPriceRestore = await get(
      "admin temporary order price restored detail",
      `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(paidPriceCandidate.orderId)}`,
      token
    );
    assertTruthy("temporary order price paid restored", afterPriceRestore.paid === true, afterPriceRestore);
    assertTruthy("temporary order price restored", Number(afterPriceRestore.payPrice).toFixed(2) === Number(originalPayPrice).toFixed(2), afterPriceRestore);
    assertTruthy("temporary order price alter flag restored", (afterPriceRestore.isAlterPrice === true ? 1 : 0) === Number(originalIsAlterPrice || 0), afterPriceRestore);
    const priceLeftovers = Number(runMysql(`
      select count(1)
      from eb_store_order_status s
      inner join eb_store_order o on o.id = s.oid
      where o.order_id = ${sqlString(paidPriceCandidate.orderId)}
        and s.change_type = 'edit_price'
        and s.change_message = ${sqlString(`订单改价为${nextPayPrice}元`)};
    `));
    assertTruthy("temporary order price marker cleaned", priceLeftovers === 0, { priceLeftovers, orderNo: paidPriceCandidate.orderId });
    priceWriteback = {
      orderNo: paidPriceCandidate.orderId,
      originalPayPrice,
      nextPayPrice,
      restored: true,
      leftovers: priceLeftovers,
      source: "temporary-unpaid-readback"
    };
  } else {
    priceWriteback = { skipped: "no unpaid or safe paid order in current database" };
  }
}
const globalPriceLeftovers = Number(runMysql(`
  select count(1)
  from eb_store_order_status
  where change_type = 'edit_price'
    and change_message like '订单改价为%.%元'
    and create_time >= date_sub(now(), interval 1 hour);
`));
assertTruthy("admin order price global recent markers cleaned", globalPriceLeftovers === 0, { globalPriceLeftovers });

const exportResult = await get(
  "admin order export safe local file",
  `${adminBase}/api/admin/export/excel/order?page=1&limit=5&status=all&type=2`,
  token
);
assertTruthy("admin order export local path", /^\/crmebimage\/export\/.+\.csv$/.test(String(exportResult.fileName || "")), exportResult);

const printResult = await get(
  "admin order print safe local file",
  `${adminBase}/api/admin/yly/print/${encodeURIComponent(allOrderNo)}`,
  token
);
assertTruthy("admin order print returns success", printResult === true, printResult);

const expressList = await get("admin express list", `${adminBase}/api/admin/express/list?page=1&limit=5&isShow=1`, token);
assertTruthy("admin express list shape", Array.isArray(expressList.list), expressList);
const expressSync = await post("admin express sync safe", `${adminBase}/api/admin/express/sync/express`, undefined, token);
assertTruthy("admin express sync localMode", expressSync.localMode === true && expressSync.synced === false, expressSync);

const checks = [];
const refundingOrder = await findOrderByStatus("refunding", token);
let refundRefuseWriteback = null;
if (refundingOrder) {
  const info = await get("admin refunding order info", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo(refundingOrder))}`, token);
  assertTruthy("refunding status", [1, 3].includes(Number(info.refundStatus)), info);
  assertTruthy("refunding statusStr", ["applyRefund", "refunding"].includes(info.statusStr?.key), info.statusStr);
  checks.push({ status: "refunding", orderNo: info.orderId, refundStatus: info.refundStatus, statusKey: info.statusStr?.key });

  const refuseMarker = `runtime-refund-refuse-${Date.now()}`;
  try {
    await get(
      "admin refund refuse writeback",
      `${adminBase}/api/admin/store/order/refund/refuse?${new URLSearchParams({
        orderNo: info.orderId,
        reason: refuseMarker
      })}`,
      token
    );
    const afterRefuse = await get("admin refund refuse detail readback", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(info.orderId)}`, token);
    assertTruthy("admin refund refuse status saved", Number(afterRefuse.refundStatus || 0) === 0, afterRefuse);
    assertTruthy("admin refund refuse reason saved", afterRefuse.refundReason === refuseMarker, afterRefuse);
    assertTruthy("admin refund refuse status key", afterRefuse.statusStr?.key === "refundRefused", afterRefuse.statusStr);
    const statusAfterRefuse = await get(
      "admin refund refuse status readback",
      `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(info.orderId)}&page=1&limit=20`,
      token
    );
    assertTruthy(
      "admin refund refuse status log saved",
      listRows(statusAfterRefuse).some((row) => row.changeType === "refund_refuse" && String(row.changeMessage || "").includes(refuseMarker)),
      statusAfterRefuse
    );
  } finally {
    runMysql(`
      update eb_store_order
      set refund_status = ${Number(info.refundStatus || 0)},
          refund_reason = ${sqlString(info.refundReason || "")},
          refund_price = ${sqlString(info.refundPrice ?? "0.00")},
          refund_reason_wap_img = ${sqlString(Array.isArray(info.refundReasonWapImg) ? JSON.stringify(info.refundReasonWapImg) : (info.refundReasonWapImg || ""))},
          refund_reason_wap_explain = ${sqlString(info.refundReasonWapExplain || "")},
          refund_reason_wap = ${sqlString(info.refundReasonWap || "")},
          refund_reason_time = ${sqlString(info.refundReasonTime || null)}
      where order_id = ${sqlString(info.orderId)};

      delete s
      from eb_store_order_status s
      inner join eb_store_order o on o.id = s.oid
      where o.order_id = ${sqlString(info.orderId)}
        and s.change_type = 'refund_refuse'
        and s.change_message = ${sqlString(`不退款原因：${refuseMarker}`)};
    `);
  }
  const afterRefuseRestore = await get(
    "admin refund refuse restored detail",
    `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(info.orderId)}`,
    token
  );
  assertTruthy("admin refund refuse status restored", Number(afterRefuseRestore.refundStatus) === Number(info.refundStatus), afterRefuseRestore);
  assertTruthy("admin refund refuse reason restored", (afterRefuseRestore.refundReason || "") === (info.refundReason || ""), afterRefuseRestore);
  const refuseLeftovers = Number(runMysql(`
    select count(1)
    from eb_store_order_status s
    inner join eb_store_order o on o.id = s.oid
    where o.order_id = ${sqlString(info.orderId)}
      and s.change_type = 'refund_refuse'
      and s.change_message like ${sqlString(`%${refuseMarker}%`)};
  `));
  assertTruthy("admin refund refuse status marker cleaned", refuseLeftovers === 0, { refuseLeftovers, orderNo: info.orderId, refuseMarker });
  refundRefuseWriteback = {
    orderNo: info.orderId,
    marker: refuseMarker,
    restored: true,
    leftovers: refuseLeftovers
  };
} else {
  checks.push({ status: "refunding", skipped: "no matching order in current database" });
  refundRefuseWriteback = { skipped: "no matching refunding order in current database" };
}
const globalRefundRefuseLeftovers = Number(runMysql(`
  select count(1)
  from eb_store_order_status
  where change_type = 'refund_refuse'
    and change_message like '%runtime-refund-refuse-%';
`));
assertTruthy("admin refund refuse global markers cleaned", globalRefundRefuseLeftovers === 0, { globalRefundRefuseLeftovers });

const refundedOrder = await findOrderByStatus("refunded", token);
if (refundedOrder) {
  const info = await get("admin refunded order info", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo(refundedOrder))}`, token);
  assertTruthy("refunded status", Number(info.refundStatus) === 2, info);
  assertTruthy("refunded statusStr", info.statusStr?.key === "refunded", info.statusStr);
  checks.push({ status: "refunded", orderNo: info.orderId, refundStatus: info.refundStatus, statusKey: info.statusStr?.key });
} else {
  checks.push({ status: "refunded", skipped: "no matching order in current database" });
}

const refusedOrder = await findOrderByStatus("refundRefused", token);
if (refusedOrder) {
  const info = await get("admin refused refund order info", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo(refusedOrder))}`, token);
  assertTruthy("refused refund status", Number(info.refundStatus || 0) === 0 && Boolean(info.refundReason), info);
  assertTruthy("refused refund statusStr", info.statusStr?.key === "refundRefused", info.statusStr);
  checks.push({ status: "refundRefused", orderNo: info.orderId, refundReason: info.refundReason, statusKey: info.statusStr?.key });
} else {
  checks.push({ status: "refundRefused", skipped: "no matching order in current database" });
}

const notShippedOrder = await findOrderByStatus("notShipped", token);
let deliveryWriteback = null;
if (notShippedOrder) {
  const info = await get("admin not shipped order info", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo(notShippedOrder))}`, token);
  assertTruthy("not shipped can be delivered", info.paid === true && Number(info.refundStatus || 0) === 0 && Number(info.shippingType) === 1, info);
  checks.push({ status: "notShipped", orderNo: info.orderId, deliveryType: info.deliveryType || "" });

  const deliveryMarker = `runtime-delivery-${Date.now()}`;
  try {
    await post(
      "admin order delivery writeback",
      `${adminBase}/api/admin/store/order/send`,
      {
        orderNo: info.orderId,
        deliveryType: "noNeed",
        marker: deliveryMarker
      },
      token
    );
    const afterDelivery = await get(
      "admin order delivery detail readback",
      `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(info.orderId)}`,
      token
    );
    assertTruthy("admin order delivery status saved", Number(afterDelivery.status) === 1, afterDelivery);
    assertTruthy("admin order delivery type saved", afterDelivery.deliveryType === "noNeed", afterDelivery);
    assertTruthy("admin order delivery record type saved", Number(afterDelivery.expressRecordType || 0) === 0, afterDelivery);
    const statusAfterDelivery = await get(
      "admin order delivery status readback",
      `${adminBase}/api/admin/store/order/status/list?orderNo=${encodeURIComponent(info.orderId)}&page=1&limit=20`,
      token
    );
    assertTruthy(
      "admin order delivery status log saved",
      listRows(statusAfterDelivery).some((row) => row.changeType === "delivery" && String(row.changeMessage || "") === "订单无需发货"),
      statusAfterDelivery
    );
  } finally {
    runMysql(`
      update eb_store_order
      set status = ${Number(info.status || 0)},
          delivery_type = ${sqlString(info.deliveryType || "")},
          delivery_name = ${sqlString(info.deliveryName || "")},
          delivery_id = ${sqlString(info.deliveryId || "")},
          delivery_code = ${sqlString(info.deliveryCode || "")},
          express_record_type = ${info.expressRecordType === null || info.expressRecordType === undefined ? "null" : Number(info.expressRecordType)}
      where order_id = ${sqlString(info.orderId)};

      delete s
      from eb_store_order_status s
      inner join eb_store_order o on o.id = s.oid
      where o.order_id = ${sqlString(info.orderId)}
        and s.change_type = 'delivery'
        and s.change_message = '订单无需发货';
    `);
  }
  const afterDeliveryRestore = await get(
    "admin order delivery restored detail",
    `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(info.orderId)}`,
    token
  );
  assertTruthy("admin order delivery status restored", Number(afterDeliveryRestore.status) === Number(info.status || 0), afterDeliveryRestore);
  assertTruthy("admin order delivery type restored", (afterDeliveryRestore.deliveryType || "") === (info.deliveryType || ""), afterDeliveryRestore);
  const deliveryLeftovers = Number(runMysql(`
    select count(1)
    from eb_store_order_status s
    inner join eb_store_order o on o.id = s.oid
    where o.order_id = ${sqlString(info.orderId)}
      and s.change_type = 'delivery'
      and s.change_message = '订单无需发货';
  `));
  assertTruthy("admin order delivery marker cleaned", deliveryLeftovers === 0, { deliveryLeftovers, orderNo: info.orderId, deliveryMarker });
  deliveryWriteback = {
    orderNo: info.orderId,
    deliveryType: "noNeed",
    restored: true,
    leftovers: deliveryLeftovers
  };
} else {
  checks.push({ status: "notShipped", skipped: "no matching order in current database" });
  deliveryWriteback = { skipped: "no matching not shipped order in current database" };
}
const globalDeliveryLeftovers = Number(runMysql(`
  select count(1)
  from eb_store_order_status
  where change_type = 'delivery'
    and change_message = '订单无需发货'
    and create_time >= date_sub(now(), interval 1 hour);
`));
assertTruthy("admin order delivery global recent markers cleaned", globalDeliveryLeftovers === 0, { globalDeliveryLeftovers });

const writeOffOrder = await findOrderByStatus("toBeWrittenOff", token);
let writeOffReadback = null;
if (writeOffOrder) {
  const code = writeOffOrder.verifyCode;
  assertTruthy("writeoff order has verifyCode", code, writeOffOrder);
  const info = await get("admin writeoff confirm", `${adminBase}/api/admin/store/order/writeConfirm/${encodeURIComponent(code)}`, token);
  assertTruthy("writeoff confirm matches order", info.orderId === orderNo(writeOffOrder) && Number(info.shippingType) === 2, info);
  checks.push({ status: "toBeWrittenOff", orderNo: info.orderId, verifyCode: code });
  writeOffReadback = { orderNo: info.orderId, verifyCode: code, source: "current-status" };
} else {
  const completedWriteOffOrder = queryOne(`
    select id, order_id, paid, status, refund_status, pay_price, is_alter_price
    from eb_store_order
    where paid = 1
      and refund_status = 0
      and shipping_type = 2
      and is_del = 0
      and is_system_del = 0
      and verify_code <> ''
    order by id desc
    limit 1;
  `);
  if (completedWriteOffOrder) {
    const originalWriteOffStatus = completedWriteOffOrder.status;
    const originalWriteOffRefundStatus = completedWriteOffOrder.refundStatus;
    const originalWriteOffIsDel = Number(runMysql(`
      select is_del
      from eb_store_order
      where order_id = ${sqlString(completedWriteOffOrder.orderId)}
      limit 1;
    `));
    const verifyCode = runMysql(`
      select verify_code
      from eb_store_order
      where order_id = ${sqlString(completedWriteOffOrder.orderId)}
      limit 1;
    `);
    try {
      runMysql(`
        update eb_store_order
        set status = 0,
            refund_status = 0,
            is_del = 0
        where order_id = ${sqlString(completedWriteOffOrder.orderId)};
      `);
      const tempWriteOffOrder = await findOrderByStatus("toBeWrittenOff", token);
      assertTruthy("temporary writeoff order listed", orderNo(tempWriteOffOrder) === completedWriteOffOrder.orderId, tempWriteOffOrder);
      const info = await get("admin temporary writeoff confirm", `${adminBase}/api/admin/store/order/writeConfirm/${encodeURIComponent(verifyCode)}`, token);
      assertTruthy("temporary writeoff confirm matches order", info.orderId === completedWriteOffOrder.orderId && Number(info.shippingType) === 2, info);
      assertTruthy("temporary writeoff status key", info.statusStr?.key === "toBeWrittenOff", info.statusStr);
      checks.push({ status: "toBeWrittenOff", orderNo: info.orderId, verifyCode, restored: true, source: "temporary-readback" });
      writeOffReadback = { orderNo: info.orderId, verifyCode, restored: true, source: "temporary-readback" };
    } finally {
      runMysql(`
        update eb_store_order
        set status = ${Number(originalWriteOffStatus || 0)},
            refund_status = ${Number(originalWriteOffRefundStatus || 0)},
            is_del = ${Number(originalWriteOffIsDel || 0)}
        where order_id = ${sqlString(completedWriteOffOrder.orderId)};
      `);
    }
    const restoredWriteOff = await get(
      "admin temporary writeoff restored detail",
      `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(completedWriteOffOrder.orderId)}`,
      token
    );
    assertTruthy("temporary writeoff status restored", Number(restoredWriteOff.status) === Number(originalWriteOffStatus || 0), restoredWriteOff);
    assertTruthy("temporary writeoff refund restored", Number(restoredWriteOff.refundStatus) === Number(originalWriteOffRefundStatus || 0), restoredWriteOff);
  } else {
    checks.push({ status: "toBeWrittenOff", skipped: "no matching order in current database" });
    writeOffReadback = { skipped: "no matching order in current database" };
  }
}

const deliveredOrder = await get(
  "admin delivered order sample",
  `${adminBase}/api/admin/store/order/list?page=1&limit=5&status=spike&type=2`,
  token
);
const delivered = listRows(deliveredOrder).find((row) => row.deliveryType === "express" || row.deliveryId || row.deliveryName);
let deliveredReadback = null;
if (delivered) {
  const info = await get("admin delivered order detail", `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(orderNo(delivered))}`, token);
  assertTruthy("delivered order has delivery fields", Boolean(info.deliveryType) || Boolean(info.deliveryId) || Boolean(info.deliveryName), info);
  checks.push({
    status: "delivered",
    orderNo: info.orderId,
    deliveryType: info.deliveryType,
    deliveryName: info.deliveryName,
    deliveryId: info.deliveryId,
    deliveryCode: info.deliveryCode
  });
  deliveredReadback = { orderNo: info.orderId, source: "current-status", deliveryType: info.deliveryType, deliveryName: info.deliveryName, deliveryId: info.deliveryId };
} else {
  const candidate = queryOrderDeliveryCandidate();
  if (candidate) {
    const deliveryName = "验收物流";
    const deliveryId = `runtime-delivery-readback-${Date.now()}`;
    const deliveryCode = "TEST";
    try {
      runMysql(`
        update eb_store_order
        set status = 1,
            refund_status = 0,
            is_del = 0,
            delivery_type = 'express',
            delivery_name = ${sqlString(deliveryName)},
            delivery_id = ${sqlString(deliveryId)},
            delivery_code = ${sqlString(deliveryCode)},
            express_record_type = 0
        where order_id = ${sqlString(candidate.orderId)};
      `);
      const tempDeliveredOrder = await get(
        "admin temporary delivered order sample",
        `${adminBase}/api/admin/store/order/list?page=1&limit=10&status=spike&type=2`,
        token
      );
      const tempDelivered = listRows(tempDeliveredOrder).find((row) => orderNo(row) === candidate.orderId);
      assertTruthy("temporary delivered order listed", tempDelivered, tempDeliveredOrder);
      assertTruthy("temporary delivered list fields", tempDelivered.deliveryType === "express" && tempDelivered.deliveryId === deliveryId, tempDelivered);
      const info = await get(
        "admin temporary delivered order detail",
        `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(candidate.orderId)}`,
        token
      );
      assertTruthy("temporary delivered detail type", info.deliveryType === "express", info);
      assertTruthy("temporary delivered detail name", info.deliveryName === deliveryName, info);
      assertTruthy("temporary delivered detail id", info.deliveryId === deliveryId, info);
      assertTruthy("temporary delivered detail code", info.deliveryCode === deliveryCode, info);
      checks.push({
        status: "delivered",
        orderNo: info.orderId,
        deliveryType: info.deliveryType,
        deliveryName: info.deliveryName,
        deliveryId: info.deliveryId,
        deliveryCode: info.deliveryCode,
        restored: true,
        source: "temporary-readback"
      });
      deliveredReadback = {
        orderNo: info.orderId,
        deliveryType: info.deliveryType,
        deliveryName: info.deliveryName,
        deliveryId: info.deliveryId,
        deliveryCode: info.deliveryCode,
        restored: true,
        source: "temporary-readback"
      };
    } finally {
      runMysql(`
        update eb_store_order
        set status = ${Number(candidate.status || 0)},
            refund_status = ${Number(candidate.refundStatus || 0)},
            is_del = ${Number(candidate.isDel || 0)},
            delivery_type = ${sqlString(candidate.deliveryType || "")},
            delivery_name = ${sqlString(candidate.deliveryName || "")},
            delivery_id = ${sqlString(candidate.deliveryId || "")},
            delivery_code = ${sqlString(candidate.deliveryCode || "")},
            express_record_type = ${candidate.expressRecordType === null || candidate.expressRecordType === undefined ? "null" : Number(candidate.expressRecordType)}
        where order_id = ${sqlString(candidate.orderId)};
      `);
    }
    const restored = await get(
      "admin temporary delivered restored detail",
      `${adminBase}/api/admin/store/order/info?orderNo=${encodeURIComponent(candidate.orderId)}`,
      token
    );
    assertTruthy("temporary delivered status restored", Number(restored.status) === Number(candidate.status || 0), restored);
    assertTruthy("temporary delivered refund restored", Number(restored.refundStatus || 0) === Number(candidate.refundStatus || 0), restored);
    assertTruthy("temporary delivered type restored", (restored.deliveryType || "") === (candidate.deliveryType || ""), restored);
    assertTruthy("temporary delivered id restored", (restored.deliveryId || "") === (candidate.deliveryId || ""), restored);
    const readbackLeftovers = Number(runMysql(`
      select count(1)
      from eb_store_order
      where delivery_id like 'runtime-delivery-readback-%';
    `));
    assertTruthy("temporary delivered marker cleaned", readbackLeftovers === 0, { readbackLeftovers, orderNo: candidate.orderId });
  } else {
    checks.push({ status: "delivered", skipped: "no shippable order in current database" });
    deliveredReadback = { skipped: "no shippable order in current database" };
  }
}

const refundImages = normalizeImages(detail.refundReasonWapImg);
for (const image of refundImages) {
  assertTruthy("refund voucher image path normalized", assetLooksLocalOrSafe(image), { image, orderNo: detail.orderId });
}

console.log(JSON.stringify({
  runtimeOrderSafeSmokeOk: true,
  adminBase,
  unauthorized,
  statusNum: {
    all: statusNum.all,
    notShipped: statusNum.notShipped,
    toBeWrittenOff: statusNum.toBeWrittenOff,
    refunding: statusNum.refunding,
    refunded: statusNum.refunded,
    refundRefused: statusNum.refundRefused
  },
  checkedOrderNo: allOrderNo,
  markWriteback: {
    marker: markMarker,
    restored: markRestored,
    leftovers: markLeftovers,
    globalLeftovers: globalMarkLeftovers
  },
  priceWriteback: {
    ...priceWriteback,
    globalRecentLeftovers: globalPriceLeftovers
  },
  refundRefuseWriteback: {
    ...refundRefuseWriteback,
    globalLeftovers: globalRefundRefuseLeftovers
  },
  deliveryWriteback: {
    ...deliveryWriteback,
    globalRecentLeftovers: globalDeliveryLeftovers
  },
  deliveredReadback,
  writeOffReadback,
  exportFile: exportResult.fileName,
  expressSync: {
    localMode: expressSync.localMode,
    synced: expressSync.synced
  },
  checks
}, null, 2));
