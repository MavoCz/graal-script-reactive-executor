(async function fetch() {
    const company = await client.get('/company/info');
    console.log("Status for company info: " + company.status);
    const ceoList = await client.get('/company/ceo');
    console.log("Status for ceo list: " + ceoList.status);

    return {
        company: JSON.parse(company.data),
        ceos: JSON.parse(ceoList.data)
    }
})
