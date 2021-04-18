(async function fetch() {
    const company = await client.get('/company/info/doesnotexist');
    console.log(company.status);
    const ceoList = await client.get('/company/ceo');
    console.log(ceoList.status);

    return {
        company: company.json(),
        ceos: ceoList.json()
    }
})
