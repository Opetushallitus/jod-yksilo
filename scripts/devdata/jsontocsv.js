function jsonToCsvWithUriAndData(json) {
  if (!json.data.length) return '';
  const headers = ['esco_uri', 'data'];
  const csvRows = [headers.join(',')];
  console.log(json.data.length);
  json.data
      // skip if esco_uri empty. there is bad data in result
      .filter(obj => obj.uri && obj.uri.trim() !== '')
      .forEach(obj => {
        const esco_uri = obj.uri || '';
        const data = JSON.stringify(obj).replace(/"/g, '""');
        csvRows.push(`"${esco_uri}","${data}"`);
      });
  console.log(csvRows.length);
  return csvRows.join('\r\n');
}

// Paste tilastot result here
const jsonData = {};

const fs = require('fs');

const csv = jsonToCsvWithUriAndData(jsonData);
fs.writeFileSync('ammattiryhma.csv', csv, 'utf-8');
console.log('CSV file saved as ammattiryhma.csv');

