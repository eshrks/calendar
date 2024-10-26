import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter month: ");
		int m = input.nextInt();
		System.out.println("Enter year: ");
		int year = input.nextInt();
		
		String month = "";
//		int year = 1924;
//		int m = 1;
		int yearDigit = year;
		int monthDigit = m;
		int d = 1;
		int dayOfFirstDate = 0;
		
		switch (m) {
    	case 1: month = "January"; break;
    	case 2: month = "February"; break;
    	case 3: month = "March"; break;
    	case 4: month = "April"; break;
    	case 5: month = "May"; break;
    	case 6: month = "June"; break;
    	case 7: month = "July"; break;
    	case 8: month = "August"; break;
    	case 9: month = "September"; break;
    	case 10: month = "October"; break;
    	case 11: month = "November"; break;
    	case 12: month = "December"; break;
        }
		
		// identify month accordance
		String yearType = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ? "Leap" : "Usual";
		int[] daysOfMonthsList = {31, yearType == "Leap" ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		
		// prerequisite equation before following the formula
        if (m == 1 || m == 2) {
        	m = m + 12;
        	year = year - 1;
        }
        
        // determining the century and year in century
        int j = year / 100;
        int k = year % 100;
        
        // formula for Zeller's Congruence Theorem Modified Version
        int h = (d + 
        		(26 * (m + 1) / 10) + 
        		k + 
        		(k / 4) + 
        		(j / 4) + 
        		5 * j) % 7;
        
        switch (h) {
        case 0: dayOfFirstDate = 6; break;
    	case 1: dayOfFirstDate = 7; break;
    	case 2: dayOfFirstDate = 1; break;
    	case 3: dayOfFirstDate = 2; break;
    	case 4: dayOfFirstDate = 3; break;
    	case 5: dayOfFirstDate = 4; break;
    	case 6: dayOfFirstDate = 5; break;
        }
		
		
		
		
		
		// Calendar Generation
		String header = """
				 ____________________
				|      %s %4d      |
				|__ __ __ __ __ __ __|
				|Mo|Tu|We|Th|Fr|Sa|Su|
				""";
		
		
		int dayCounter = 0;
		System.out.printf(header, month.substring(0, 3), yearDigit); // header
		System.out.print("|");
		
		for (int i = 1; i <= daysOfMonthsList[monthDigit-1]; i++) { // it'll loop based on days in months
			if (dayCounter != 7) {
				for (int q; dayOfFirstDate != 1; dayOfFirstDate--) {
					System.out.print("  |");
					dayCounter++;
				}
				System.out.printf("%2d|", i);
				dayCounter++;
			} 
			else {
				System.out.println();
				System.out.printf("|%2d|", i);
				dayCounter = 1;
			}
		}
		for (int i = dayCounter; dayCounter != 7; dayCounter++) {
			System.out.print("  |");
		}
		System.out.println();
		System.out.println("----------------------");

	}

}
