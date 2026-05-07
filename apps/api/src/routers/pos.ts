// POS Router — mirrors PrintFlowLite's PosItemService + PosPurchaseService
import type { FastifyInstance, FastifyRequest, FastifyReply } from "fastify";
import { z } from "zod";
import { v4 as uuidv4 } from "uuid";

export interface PosItem {
  id: number;
  name: string;
  cost: number;
  category?: string;
  isActive: boolean;
  createdAt: string;
}

export interface PosPurchaseItem {
  itemIndex: number;
  name: string;
  quantity: number;
  unitCost: number;
}

export interface PosPurchase {
  id: number;
  uuid: string;
  receiptNumber: string;
  userId: number | null;
  userName: string | null;
  items: PosPurchaseItem[];
  totalCost: number;
  paymentType: string;
  comment: string | null;
  accountTrxId: number | null;
  createdAt: string;
}

let posItems: PosItem[] = [
  { id: 1, name: "A4 Print (B&W)", cost: 0.10, category: "printing", isActive: true, createdAt: new Date().toISOString() },
  { id: 2, name: "A4 Print (Color)", cost: 0.50, category: "printing", isActive: true, createdAt: new Date().toISOString() },
  { id: 3, name: "A3 Print (B&W)", cost: 0.20, category: "printing", isActive: true, createdAt: new Date().toISOString() },
  { id: 4, name: "A3 Print (Color)", cost: 1.00, category: "printing", isActive: true, createdAt: new Date().toISOString() },
  { id: 5, name: "Binding (Spiral)", cost: 2.00, category: "finishing", isActive: true, createdAt: new Date().toISOString() },
  { id: 6, name: "Laminating (A4)", cost: 1.50, category: "finishing", isActive: true, createdAt: new Date().toISOString() },
  { id: 7, name: "Scanning (B&W)", cost: 0.05, category: "scanning", isActive: true, createdAt: new Date().toISOString() },
  { id: 8, name: "Scanning (Color)", cost: 0.15, category: "scanning", isActive: true, createdAt: new Date().toISOString() },
];

let posPurchases: PosPurchase[] = [];
let purchaseIdCounter = 1;
let receiptCounter = 1000;

function nextReceiptNumber(): string {
  return `P${String(++receiptCounter).padStart(6, "0")}`;
}

const CreatePosItemSchema = z.object({
  name: z.string().min(1),
  cost: z.number().positive(),
  category: z.string().optional(),
});

const UpdatePosItemSchema = z.object({
  name: z.string().min(1).optional(),
  cost: z.number().positive().optional(),
  category: z.string().optional(),
  isActive: z.boolean().optional(),
});

const PosSaleSchema = z.object({
  userId: z.number().optional(),
  items: z.array(
    z.object({
      itemId: z.number(),
      quantity: z.number().min(1),
    }),
  ).min(1),
  paymentType: z.enum(["CASH", "CARD", "ACCOUNT", "ONLINE"]).default("CASH"),
  comment: z.string().optional(),
});

const DepositSchema = z.object({
  userId: z.number(),
  amount: z.number().positive(),
  paymentType: z.enum(["CASH", "CARD", "ONLINE"]).default("CASH"),
  comment: z.string().optional(),
  receiptDelivery: z.enum(["NONE", "EMAIL"]).default("NONE"),
});

export async function createPosRouter(app: FastifyInstance) {
  app.get("/items", async (_request: FastifyRequest, reply: FastifyReply) => {
    return reply.send({
      success: true,
      data: { items: posItems.filter((i) => i.isActive) },
      timestamp: new Date().toISOString(),
    });
  });

  app.get("/items/all", async (_request: FastifyRequest, reply: FastifyReply) => {
    return reply.send({
      success: true,
      data: { items: posItems },
      timestamp: new Date().toISOString(),
    });
  });

  app.post(
    "/items",
    { schema: { body: CreatePosItemSchema } },
    async (request: FastifyRequest<{ Body: z.infer<typeof CreatePosItemSchema> }>, reply: FastifyReply) => {
      const body = request.body;
      const newItem: PosItem = {
        id: posItems.length + 1,
        name: body.name,
        cost: body.cost,
        category: body.category,
        isActive: true,
        createdAt: new Date().toISOString(),
      };
      posItems.push(newItem);
      return reply.send({ success: true, data: { item: newItem }, timestamp: new Date().toISOString() });
    },
  );

  app.put(
    "/items/:id",
    { schema: { body: UpdatePosItemSchema } },
    async (request: FastifyRequest<{ Params: { id: string }; Body: z.infer<typeof UpdatePosItemSchema> }>, reply: FastifyReply) => {
      const id = parseInt(request.params.id);
      const idx = posItems.findIndex((i) => i.id === id);
      if (idx === -1) {
        return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Item not found" } });
      }
      posItems[idx] = { ...posItems[idx], ...request.body };
      return reply.send({ success: true, data: { item: posItems[idx] }, timestamp: new Date().toISOString() });
    },
  );

  app.delete("/items/:id", async (request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) => {
    const id = parseInt(request.params.id);
    const idx = posItems.findIndex((i) => i.id === id);
    if (idx === -1) {
      return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Item not found" } });
    }
    posItems[idx].isActive = false;
    return reply.send({ success: true, data: { deleted: true }, timestamp: new Date().toISOString() });
  });

  app.post(
    "/sales",
    { schema: { body: PosSaleSchema } },
    async (request: FastifyRequest<{ Body: z.infer<typeof PosSaleSchema> }>, reply: FastifyReply) => {
      const body = request.body;

      const purchaseItems: PosPurchaseItem[] = body.items.map((li, idx) => {
        const item = posItems.find((i) => i.id === li.itemId);
        if (!item) throw new Error(`Item ${li.itemId} not found`);
        return {
          itemIndex: idx + 1,
          name: item.name,
          quantity: li.quantity,
          unitCost: item.cost,
        };
      });

      const totalCost = purchaseItems.reduce((sum, i) => sum + i.quantity * i.unitCost, 0);

      const purchase: PosPurchase = {
        id: purchaseIdCounter++,
        uuid: uuidv4(),
        receiptNumber: nextReceiptNumber(),
        userId: body.userId ?? null,
        userName: null,
        items: purchaseItems,
        totalCost,
        paymentType: body.paymentType,
        comment: body.comment ?? null,
        accountTrxId: null,
        createdAt: new Date().toISOString(),
      };

      posPurchases.unshift(purchase);

      return reply.send({
        success: true,
        data: { purchase },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.post(
    "/deposits",
    { schema: { body: DepositSchema } },
    async (request: FastifyRequest<{ Body: z.infer<typeof DepositSchema> }>, reply: FastifyReply) => {
      const body = request.body;

      const purchase: PosPurchase = {
        id: purchaseIdCounter++,
        uuid: uuidv4(),
        receiptNumber: `D${String(++receiptCounter).padStart(6, "0")}`,
        userId: body.userId,
        userName: null,
        items: [
          {
            itemIndex: 1,
            name: "Account Deposit",
            quantity: 1,
            unitCost: body.amount,
          },
        ],
        totalCost: body.amount,
        paymentType: body.paymentType,
        comment: body.comment ?? null,
        accountTrxId: null,
        createdAt: new Date().toISOString(),
      };

      posPurchases.unshift(purchase);

      return reply.send({
        success: true,
        data: { purchase },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get(
    "/purchases",
    {
      schema: {
        querystring: z.object({
          userId: z.string().optional(),
          page: z.coerce.number().default(1),
          limit: z.coerce.number().default(20),
          dateFrom: z.string().optional(),
          dateTo: z.string().optional(),
        }),
      },
    },
    async (request: FastifyRequest<{ Querystring: any }>, reply: FastifyReply) => {
      const q = request.query;
      let filtered = [...posPurchases];

      if (q.userId) {
        filtered = filtered.filter((p) => p.userId === parseInt(q.userId));
      }
      if (q.dateFrom) {
        const from = new Date(q.dateFrom);
        filtered = filtered.filter((p) => new Date(p.createdAt) >= from);
      }
      if (q.dateTo) {
        const to = new Date(q.dateTo);
        filtered = filtered.filter((p) => new Date(p.createdAt) <= to);
      }

      const page = q.page;
      const limit = q.limit;
      const total = filtered.length;
      const paginated = filtered.slice((page - 1) * limit, page * limit);

      return reply.send({
        success: true,
        data: {
          data: paginated,
          total,
          page,
          limit,
          totalPages: Math.ceil(total / limit),
        },
        timestamp: new Date().toISOString(),
      });
    },
  );

  app.get("/summary", async (_request: FastifyRequest, reply: FastifyReply) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayPurchases = posPurchases.filter((p) => new Date(p.createdAt) >= today);

    const totalRevenue = posPurchases.reduce((sum, p) => sum + p.totalCost, 0);
    const todayRevenue = todayPurchases.reduce((sum, p) => sum + p.totalCost, 0);
    const totalSales = posPurchases.length;
    const todaySales = todayPurchases.length;
    const avgTransaction = totalSales > 0 ? totalRevenue / totalSales : 0;

    const revenueByPaymentType = posPurchases.reduce((acc, p) => {
      acc[p.paymentType] = (acc[p.paymentType] ?? 0) + p.totalCost;
      return acc;
    }, {} as Record<string, number>);

    const topItems = posPurchases.flatMap((p) => p.items).reduce((acc, i) => {
      acc[i.name] = (acc[i.name] ?? 0) + i.quantity * i.unitCost;
      return acc;
    }, {} as Record<string, number>);

    const topItemsSorted = Object.entries(topItems)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([name, revenue]) => ({ name, revenue }));

    return reply.send({
      success: true,
      data: {
        totalRevenue,
        todayRevenue,
        totalSales,
        todaySales,
        avgTransaction,
        revenueByPaymentType,
        topItems: topItemsSorted,
        activeItems: posItems.filter((i) => i.isActive).length,
      },
      timestamp: new Date().toISOString(),
    });
  });

  app.get("/receipt/:uuid", async (request: FastifyRequest<{ Params: { uuid: string } }>, reply: FastifyReply) => {
    const purchase = posPurchases.find((p) => p.uuid === request.params.uuid);
    if (!purchase) {
      return reply.status(404).send({ success: false, error: { code: "NOT_FOUND", message: "Receipt not found" } });
    }
    return reply.send({ success: true, data: { purchase }, timestamp: new Date().toISOString() });
  });
}
