const { Client } = require('pg');
const axios = require('axios');

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
  console.log('Found user:', user);
  
  if (!user) return console.log('No user found');

  try {
    const loginRes = await axios.post('http://localhost:8080/api/v1/auth/login', {
      correo: user.correo,
      contrasena: 'password' // Typically seeded passwords are 'password' or similar
    });
    console.log('Login success! Token:', loginRes.data.token.substring(0, 15) + '...');
    
    // Now make the PATCH request to the fixed endpoint!
    const patchRes = await axios.patch(
      'http://localhost:8080/api/v1/admin/sedes/1/beneficio-vip-cumpleanos?habilitado=true',
      null,
      { headers: { Authorization: `Bearer ${loginRes.data.token}` } }
    );
    console.log('Patch success! Status:', patchRes.status);
    
    // Check DB
    const checkRes = await client.query("SELECT vip_cumpleanos_habilitado FROM sedes WHERE id = 1");
    console.log('DB Sede 1 vip_cumpleanos_habilitado:', checkRes.rows[0]);
  } catch (err) {
    console.error('Error during API calls:', err.response?.data || err.message);
  } finally {
    await client.end();
  }
}
test();
