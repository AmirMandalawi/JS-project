window.addEventListener("load", init);
function init() {
    clearAll();
    loadId();
    showTotal();
    bindEvents();
}

function clearAll() {
    /* this function clears the contents of the form except the ID (since ID is auto generated)*/
    document.querySelector('#name').value = "";
    document.querySelector('#price').value = "";
    document.querySelector('#desc').value = "";
    document.querySelector('#color').value = "#000000";
    document.querySelector('#url').value = "";
}

let auto = autoGen();

function loadId() {
    /* this function automatically sets the value of ID */
    document.querySelector('#id').innerText = auto.next().value;
}

function showTotal() {
    /* this function populates the values of #total, #mark and #unmark ids of the form */
    document.querySelector('#total').innerText = itemOperations.items.length;
    document.querySelector('#mark').innerText = itemOperations.countTotalMarked();
    document.querySelector('#unmark').innerText = itemOperations.items.length - itemOperations.countTotalMarked();
}

function bindEvents() {
    document.querySelector('#remove').addEventListener('click', deleteRecords);
    document.querySelector('#add').addEventListener('click', addRecord);
    document.querySelector('#update').addEventListener('click', updateRecord)
    document.querySelector('#exchange').addEventListener('change', getExchangerate)
    document.querySelector('#save').addEventListener('click', saveToLocal)
    document.querySelector('#load').addEventListener('click', loadFromLocal)
    document.querySelector('#savejson').addEventListener('click', saveToJson)
    document.querySelector('#loadjson').addEventListener('click', loadFromJson)
    document.querySelector('#savemongo').addEventListener('click', saveToMongo)
    document.querySelector('#loadmongo').addEventListener('click', loadFromMongo)
}

function deleteRecords() {
    /* this function deletes the selected record from itemOperations and prints the table using the function printTable*/
    var tbody = document.querySelector('#items');
    tbody.innerHTML = "";
    itemOperations.remove();
    printTable(itemOperations.items);
    showTotal();
    console.log("I am Delete ", this.getAttribute('data-itemid'));
}

function addRecord() {
    /* this function adds a new record in itemOperations and then calls printRecord(). showTotal(), loadId() and clearAll()*/
    var item = new Item();
    item.id = document.querySelector('#id').innerText;
    item.name = document.querySelector('#name').value;
    item.desc = document.querySelector('#desc').value;
    item.price = document.querySelector('#price').value;
    item.color = document.querySelector('#color').value;
    item.url = document.querySelector('#url').value;
    itemOperations.add(item);
    printRecord(item);
    showTotal();
    loadId();
    clearAll();
}

function edit() {
    /*this function fills (calls fillFields()) the form with the values of the item to edit after searching it in items */
    let id = this.getAttribute('data-itemid');
    let itemObject = itemOperations.search(id);
    fillFields(itemObject);
    console.log("I am Edit ", this.getAttribute('data-itemid'));
}

function fillFields(itemObject) {
    /*this function fills the form with the details of itemObject*/
    document.querySelector('#id').innerText = itemObject.id;
    document.querySelector('#name').value = itemObject.name;
    document.querySelector('#price').value = itemObject.price;
    document.querySelector('#desc').value = itemObject.desc;
    document.querySelector('#color').value = itemObject.color;
    document.querySelector('#url').value = itemObject.url;
}

function createIcon(className, fn, id) {
    /* this function creates icons for edit and trash for each record in the table*/
    // <i class="fas fa-trash"></i>
    // <i class="fas fa-edit"></i>
    var iTag = document.createElement("i");
    iTag.className = className;
    iTag.addEventListener('click', fn);
    iTag.setAttribute("data-itemid", id);
    return iTag;
}

function updateRecord() {
    /*this function updates the record that is edited and then prints the table using printTable()*/
    var tbody = document.querySelector('#items');
    tbody.innerHTML = "";
    var item = new Item();
    item.id = document.querySelector('#id').innerText;
    item.name = document.querySelector('#name').value;
    item.desc = document.querySelector('#desc').value;
    item.price = document.querySelector('#price').value;
    item.color = document.querySelector('#color').value;
    item.url = document.querySelector('#url').value;
    itemOperations.update(item);
    printTable(itemOperations.items);
    showTotal();
    clearAll();
    console.log("I am Update ", this.getAttribute('data-itemid'));
}

function trash() {
    /*this function toggles the color of the row when its trash button is selected and updates the marked and unmarked fields */
    let id = this.getAttribute('data-itemid');
    itemOperations.markUnMark(id);
    showTotal();
    let tr = this.parentNode.parentNode;
    tr.classList.toggle('alert-danger');
    console.log("I am Trash ", this.getAttribute('data-itemid'))
}

function printTable(items) {
    /* this function calls printRecord for each item of items and then calls the showTotal function*/
    items.forEach(key => printRecord(key));
    showTotal();
}

function printRecord(item) {
    var tbody = document.querySelector('#items');
    var tr = tbody.insertRow();
    var index = 0;
    for (let key in item) {
        if (key == 'isMarked') {
            continue;
        }
        let cell = tr.insertCell(index);
        cell.innerText = item[key];
        index++;
    }
    var lastTD = tr.insertCell(index);
    lastTD.appendChild(createIcon('fas fa-trash mr-2', trash, item.id));
    lastTD.appendChild(createIcon('fas fa-edit', edit, item.id));
}

function getExchangerate() {
    /* this function makes an AJAX call to http://apilayer.net/api/live to fetch and display the exchange rate for the currency selected*/
    let selectedCurrency = document.getElementById("exchange").value;
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "http://apilayer.net/api/live?access_key=e272d327449e8e161d33bff9c20ab749&source=USD&currencies=" + selectedCurrency + "&format=1", true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                var response = JSON.parse(xhr.responseText);
                var exchangeRate = response.quotes["USD" + selectedCurrency];
                document.getElementById("exrate").textContent = exchangeRate.toFixed(2);
            } else {
                document.getElementById("exrate").textContent = "Error fetching exchange rate.";
            }
        }
    };
    xhr.send();
}

function saveToLocal() {
    /* this function saves the items array to local storage*/
    localStorage.setItem("items", JSON.stringify(itemOperations.items));
    console.log(localStorage.getItem("items"));
    console.log("I am Save ", this.getAttribute('data-itemid'));
}

function loadFromLocal() {
    /* this function loads the items array from local storage*/
    itemOperations.items = JSON.parse(localStorage.getItem("items"));
    printTable(itemOperations.items);
    showTotal();
    console.log("I am Load ", this.getAttribute('data-itemid'));
}

function saveToJson() {
    const savedData = itemOperations.items;
    if (savedData) {
        console.log(savedData);
        fetch('http://localhost:3000/api/data', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(savedData),
        })
            .then(response => response.json())
            .then(data => {
                console.log(data.message);
                dataInput.value = '';
            })
            .catch(error => console.error(error));
    }
}

function saveToMongo() {
    const savedData = itemOperations.items;
    if (savedData) {
        fetch("http://localhost:3000/api/postMongo", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(savedData), // Convert JavaScript object to JSON string
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json(); // Parse the JSON response
            })
            .then((responseData) => {
                console.log(responseData.message); // Handle the server response
            })
            .catch((error) => {
                console.error("Error:", error);
            });
    }
}

function loadFromJson() {
    fetch('http://localhost:3000/api/data')
        .then(response => response.json())
        .then(data => {
            console.log(data);
            itemOperations.items = data;
            printTable(itemOperations.items);
            loadId();
            deleteRecords();
        })
        .catch(error => console.error(error));
}

function loadFromMongo() {
    fetch('http://localhost:3000/api/getMongo')
        .then(response => response.json())
        .then(data => {
            console.log(data);
            itemOperations.items = data;
            printTable(itemOperations.items);
            loadId();
            deleteRecords();
        })
        .catch(error => console.error(error));
}