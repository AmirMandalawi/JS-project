const itemOperations = {
    items: [],
    add(itemObject) {
        /* adds an item into the array items*/
        this.items.push(itemObject);
    },
    remove() {
        /* removes the item which has the "isMarked" field set to true*/
        this.items = this.items.filter(itemObject => itemObject.isMarked == false);
        return this.items;
    },
    search(id) {
        /* searches the item with a given argument id */
        return this.items.find(itemObject => itemObject.id == id);
    },
    markUnMark(id) {
        /* toggle the isMarked field of the item with the given argument id*/
        var itemObject = this.search(id);
        itemObject.isMarked = !itemObject.isMarked;
        return itemObject.isMarked;
    },
    countTotalMarked() {
        /* counts the total number of marked items */
        var count = 0;
        this.items.forEach(itemObject => {
            if (itemObject.isMarked) {
                count++;
            }
        }
        );
        return count;
    },
    update(itemObject) {
        if (this.search(itemObject.id) == undefined) {
            console.log("Item not found");
        } else {
            let removeitem = this.search(itemObject.id);
            this.items.splice(this.items.indexOf(removeitem), 1, itemObject);
        }
        return this.items;
    },

}