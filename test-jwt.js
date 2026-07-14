const jwt = require('jsonwebtoken');
const axios = require('axios');
const { Client } = require('pg');

async function test() {
  const client = new Client({
    user: 'postgres',
    host: 'localhost',
    database: 'cinezone_db',
    password: 'blae',
    port: 5432,
  });
  await client.connect();
  const res = await client.query("SELECT correo, rol FROM usuarios WHERE rol IN ('SUPER_ADMIN', 'ADMIN_SEDE') LIMIT 1");
  const user = res.rows[0];
  if (!user) return console.log('No admin users found in DB!');

  const payload = {
    sub: user.correo,
    rol: "ADMIN_SEDE",
    userId: "1"
  };
  const secretBuffer = Buffer.from("dGhpc2lzYXNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3Jqd3Q=", "base64");
  const token = jwt.sign(payload, secretBuffer, { algorithm: 'HS256' });

  console.log("Generated token:", token);

  try {
    const patchRes = await axios.patch(
      'http://localhost:8080/api/v1/admin/sedes/1/beneficio-vip-cumpleanos?habilitado=true',
      null,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log('PATCH Status:', patchRes.status);
    
    // Check DB
    const client = new Client({
      user: 'postgres',
      host: 'localhost',
      database: 'cinezone_db',
      password: 'blae',
      port: 5432,
    });
    await client.connect();
    const checkRes = await client.query("SELECT vip_cumpleanos_habilitado FROM sedes WHERE id = 1");
    console.log('DB Sede 1 vip_cumpleanos_habilitado:', checkRes.rows[0]);
    await client.end();
  } catch (err) {
    console.error('Error:', err.response?.status, err.response?.data || err.message);
  }
}
test();
