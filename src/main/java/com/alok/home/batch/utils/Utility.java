package com.alok.home.batch.utils;

import com.alok.home.commons.constant.Company;
import com.alok.home.commons.constant.UploadType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import static java.lang.StringTemplate.STR;

/**
 *
 */
@Slf4j
public class Utility {

    static Map<String, List<String>> categoryStringMap = new HashMap<>();


    static {
        categoryStringMap.put(
                "Bills", Arrays.asList("recharge", "bill", "electricity", "gas", "indane", "tatasky",
                        "broadband", "airtel", "bescom", "tata sky", "insurance", "subscription",
                        "netflix", "membership", "racharge")
        );
        categoryStringMap.put(
                "Fuel", Arrays.asList("fuel", "petrol", "patrol", "engine oil", "diesel")
        );
        categoryStringMap.put(
                "Automotive", Arrays.asList("car service", "byke service", "puncture", "car lights", "scottie", "byke",
                        "scooter", "car cleaning", "car duster"
                )
        );
        categoryStringMap.put(
                "Milk", Arrays.asList("milk", "doodh")
        );
        categoryStringMap.put(
                "Maintenance", Arrays.asList( "maintenance", "plumber", "plumb", "maintance", "service",
                        "bulb", "acqua", "repair", "aquaguard", "carpenter", "electrician", "lamp",
                        "byke", "plant", "door lock", "lights", "paint", "shower head", "curtain",
                        "hanger", "door")
        );
        categoryStringMap.put(
                "Travel", Arrays.asList("travel", "taxi","ola", "uber", "auto", "train", "flight",
                        "ticket", "hotel", "toll", "tip", "travel", "fasttag", "fastag")
        );
        categoryStringMap.put(
                "House Help", Arrays.asList("househelp", "cook", "ranjit", "ranjeet", "basantha",
                        "basanta", "maid", "byke wash", "bykewash", "byke clean", "bykeclean",
                        "vasantha", "iron", "car wash", "scooty cleaner", "wheeler cleaner",
                        "laundry", "car cleaner", "garbage")
        );
        categoryStringMap.put(
                "Food Outside", Arrays.asList("food", "pizza", "burger", "swigy", "lunch", "dinner",
                        "breakfast", "snax", "sweets", "restaurant", "idli", "southin", "party",
                        "kachori", "cookies", "dosa", "chai", "gabbar", "sanx", "vada", "mc donald",
                        "picnic", "kfc", "food", "laddoo", "beer", "wine", "vodka", "cake",
                        "sweet", "icecream", "snacks", "laddu")
        );
        categoryStringMap.put(
                "Accessories", Arrays.asList("accessories", "hoodie", "necklace", "sunglasses", "t-shirt",
                        "hat", "bag", "shirt", "shoe", "dress", "towel", "bedsheet", "pillow", "jeans",
                        "kurti", "belt", "tailor", "decathlon", "saree", "cloth", "sandal", "shopping",
                        "underwear", "trousers", "central", "kurti", "myntra", "juttis", "blanket",
                        "socks", "sleepers", "jacket", "bed sheet", "track pant", "frock", "sari"
                )
        );
        categoryStringMap.put(
                "Grocery", Arrays.asList("grossary", "veg", "bazar", "groffer", "grofer", "chicken",
                        "mutton", "fish", "egg", "total", "pooja", "paneer", "butter", "curd", "baazar",
                        "bazzar", "bazaar", "prawns", "groffars", "oil", "salt", "tea", "coffee",
                        "biscuit", "deodorant", "jamun", "mango", "retail", "finger", "grossary",
                        "tissue", "ladoo", "fruits", "shampoo", "fruit", "sugar", "groceries",
                        "grocerie", "grocery", "coconut", "sanitizer", "sanitiser", "puja samagri",
                        "nariyal", "pediasure", "pickle", "gobhi", "sabzi", "atta", "onion",
                        "garlic", "bread", "yogart", "yogurt", "mop", "juice", "cleanser", "water",
                        "Jaggery"
                )
        );
        categoryStringMap.put(
                "Education", Arrays.asList("book", "pen", "exam", "xerox", "copy", "tution", "unacedmy",
                        "ugc", "form", "economics", "aws", "stationary", "unacademy", "education", "school",
                        "tifin box", "tuition")
        );
        categoryStringMap.put(
                "Medical", Arrays.asList("medical", "medicine", "test", "doctor", "colombia", "scan",
                        "annama", "hospital", "physiotherapy", "consultation", "Physiothreapy",
                        "Physiotherepist", "ice pack", "resistance", "physical", "protein", "glucometer",
                        "nursing", "consulting", "consultancy", "consultation", "vaccine", "injection",
                        "therepist"
                )
        );
        categoryStringMap.put(
                "Grooming", Arrays.asList("grooming", "parlor", "facial", "Manicure", "hair", "pedicure",
                        "cream", "kaya", "lotion", "urban clap", "saloon", "makeup", "trimmer", "parlour",
                        "cosmetics", "cosmetic", "facepack", "make up", "face pack", "face wash", "body wash",
                        "facewash", "lipstick", "moisturizer")
        );
        categoryStringMap.put(
                "Gift", Arrays.asList("bablu", "gift", "rakhi", "donation")
        );
        categoryStringMap.put(
                "Entertainment", Arrays.asList("entertainment", "movie", "cinema", "pvr", "inox",
                        "theatre", "firestick")
        );
        categoryStringMap.put(
                "Baby Care", Arrays.asList("baby", "firstcry", "toy", "diaper", "diapers", "wipe")
        );
        categoryStringMap.put(
                "Furniture", Arrays.asList("urban ladder", "chair", "table", "sofa", "furniture")
        );
        categoryStringMap.put(
                "Appliances", Arrays.asList("appliances", "Oven", "borosil",
                        "room heater", "mixer", "watch", "steriliser")
        );
        categoryStringMap.put(
                "Electronics", Arrays.asList("mobile", "laptop", "iphone", "macbook", "smart watch", "fan", "electronic")
        );
        categoryStringMap.put(
                "Other", Arrays.asList("warmer")
        );
    }

    static public boolean isSalaryTransaction(String transation) {
        if (transation.toLowerCase().matches(".*salary.*|.*evolving.*|.*wipro.*|.*yodlee.*|.*bosch.*|.*j.p. morgan services.*|.*jpmcremittance.*")) {
            if (!transation.toLowerCase().matches(".*reimbursement.*|.*withdrawal.*" +
                    "|.*corp.trf.*|.*trip.*|.*hotel.*|.*ref: .*|.*imps.*")) {
                return true;
            }
        }
        return false;
    }

    static public boolean isFamilyTransaction(String transation) {
        if (transation.toLowerCase().matches(".*ramawatar.*|.*avinash.*|.*avin.*|.*sbinx7084.*|.*gopal.*|.*papa.*|.*31987667084.*|.*punbx0113.*" +
                "|.*3209010000019.*|.*kharagpur.*|.*mb hr.*|.*kumari  jyoti.*|.*pankaj  kumar.*|.*bihar.*" +
                "|.*yogendra  narayan.*|.*shailendra  singh.*|.*manju  devi.*|.*vivekanand  singh.*|.*shailendra  sin.*|.*kunal pri.*" +
                "|.*maa trd.*|.*maa traders.*|.*niraj kumar.*|.*pintu bhaia.*|.*sarita kumari.*|.*ritikkumar.*|.*shreya.*")) {

            if (!transation.toLowerCase().matches(".*rachna.*|.*withdrawal.*|.*9916661247@.*|.*interest.*|" +
                    ".*monthly  principal  payment.*|.*gopal  rao.*|.*int  payment  for.*|.*blocked.released.*|.*avinash  vijayv.*|.*avinash vijayv.*"
            )) {
                return true;
            }
        }
        return false;
    }

    static public boolean isReversalTransaction(String transaction) {
        if (transaction.toLowerCase().matches(".*outward  rev.*") ||
                transaction.toLowerCase().matches(".*rev:imps.*") ||
                transaction.toLowerCase().matches(".*received from.*")
        ) {
            if (!transaction.toLowerCase().matches(".*withdrawal.*")) {
                return true;
            }
        }
        return false;
    }

    static public String getExpenseCategory(String expenseHead, String expenseComment) {
        if (expenseHead.length() == 0) {
            return "";
        }
        String stringToSearch = expenseHead.toLowerCase();

        //loop through each category until a match is reached then return the category
        for (Map.Entry<String, List<String>> entry: categoryStringMap.entrySet()) {
            for (String string: entry.getValue()) {
                if (stringToSearch.contains(string))
                    return entry.getKey();
            }
        }

        return "Other";
    }

    static public String getDateFormat(String dateString) {

        if (dateString.trim().matches("....\\/...\\/..")) {
            return "yyyy/MMM/dd";
        }

        if (dateString.trim().matches("..-..-....")) {
            return "dd-MM-yyyy";
        }

        if (dateString.trim().matches("..\\/..\\/..")) {
            return "dd/MM/yy";
        }

        return "dd/MM/yyyy";
    }

    static public UploadType getUploadType(String fileName) {

        if (fileName.matches("^Report-.*.csv$|^KM52025632.*.csv$"))
            return UploadType.KotakExportedStatement;

        if (fileName.matches("^190992811_.*.txt$"))
            return UploadType.HDFCExportedStatement;

        if (fileName.matches("^Expense Sheet - Form Responses.*.csv$"))
            return UploadType.ExpenseGoogleSheet;

        if (fileName.matches("^Expense Sheet - Tax by year.*.csv$"))
            return UploadType.TaxGoogleSheet;

        if (fileName.matches("^Expense Sheet - Investment.*.csv$"))
            return UploadType.InvestmentGoogleSheet;

        return null;
    }

    static public String getCompanyName(String salaryCreditDescription) {
//        String name = "Duke";
//        String info = STR."My name is \{name}";
//        System.out.println(info);

//        return switch (salaryCreditDescription) {
//            case null -> Company.UNKNOWN.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*j.p. morgan services.*|.*jpmcremittance.*|.*jpmc.*") -> Company.JPMC.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*evolving.*|.*evoling.*") -> Company.EVOLVING.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*wipro.*") -> Company.WIPRO.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*yodlee.*") -> Company.YODLEE.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*bosch.*") -> Company.ROBERT_BOSCH.name();
//            case String creditDescription when creditDescription.toLowerCase()
//                    .matches(".*salary.*") -> Company.SUBEX.name();
//            default -> Company.UNKNOWN.name();
//        };

        if (salaryCreditDescription.toLowerCase().matches(".*j.p. morgan services.*|.*jpmcremittance.*|.*jpmc.*"))
            return Company.JPMC.name();

        if (salaryCreditDescription.toLowerCase().matches(".*evolving.*|.*evoling.*"))
            return Company.EVOLVING.name();

        if (salaryCreditDescription.toLowerCase().matches(".*wipro.*"))
            return Company.WIPRO.name();

        if (salaryCreditDescription.toLowerCase().matches(".*yodlee.*"))
            return Company.YODLEE.name();

        if (salaryCreditDescription.toLowerCase().matches(".*bosch.*"))
            return Company.ROBERT_BOSCH.name();

        if (salaryCreditDescription.toLowerCase().matches(".*salary.*"))
            return Company.SUBEX.name();

        return Company.UNKNOWN.name();
    }
}
