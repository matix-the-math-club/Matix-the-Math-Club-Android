"""The Matix video course, grade 1 -> college (1.0.8). Each lesson is about a minute:
intro card, 3-6 worked scenes, recap card. Every lesson also carries a short multiple-choice quiz
that the app shows after the video.
Preview:  python3 curriculum.py stills <id> 5,20,40
List:     python3 curriculum.py list"""
import math, sys, os
from pkit import *
import lessons as Lb
from lessons import (intro, recap, title_card, sc_count, sc_skip, sc_place, sc_compare, sc_evenodd, sc_round, sc_add, sc_sub,
                     sc_line, sc_maketen, sc_double, sc_add9, sc_column, sc_family, sc_word, sc_groups, sc_array, sc_table,
                     sc_share, sc_divgroups, sc_mulcol, sc_clock, sc_shape, sc_perimeter)
from scenes2 import *

GRADES = [('g1', 'Grade 1'), ('g2', 'Grade 2'), ('g3', 'Grade 3'), ('g4', 'Grade 4'), ('g5', 'Grade 5'), ('g6', 'Grade 6'),
          ('g7', 'Grade 7'), ('g8', 'Grade 8'), ('g9', 'Grade 9'), ('g10', 'Grade 10'), ('g11', 'Grade 11'), ('g12', 'Grade 12'),
          ('college', 'College & beyond')]
GNAME = dict(GRADES)
ACC = {'g1': G, 'g2': G, 'g3': P, 'g4': P, 'g5': B, 'g6': B, 'g7': O, 'g8': O, 'g9': PU, 'g10': PU, 'g11': AU, 'g12': AU, 'college': R}
M = '−'  # minus sign
LESSONS = []


def L(grade, id_, title, sub, scenes, rec, quiz):
    LESSONS.append(dict(id=id_, grade=grade, title=title, sub=sub, scenes=scenes, recap=rec, quiz=quiz))


def W_(lines, eq, ans, n=0, col=P): return sc_word(lines, eq, ans, n, col)


# ===================================================================== GRADE 1
L('g1', 'count_10', 'Counting to 10', 'Point at each one and say the number',
  lambda: [sc_count(3, P), sc_count(5, B), sc_count(7, G), sc_count(9, PU), sc_count(10, AU)],
  ['Touch each thing once', 'The last number you say is how many'],
  [('How many fingers on one hand?', '5', ['4', '6']), ('What comes after 7?', '8', ['6', '9']), ('Count: ●●●●●●', '6', ['5', '7'])])
L('g1', 'count_20', 'Counting to 20', 'Count in rows of ten',
  lambda: [sc_count(12, B, 10), sc_count(15, P, 10), sc_count(18, G, 10), sc_count(20, AU, 10)],
  ['10 and 2 more is 12', 'Two full rows of 10 make 20'],
  [('10 and 4 more is…', '14', ['13', '15']), ('What comes after 19?', '20', ['18', '21']), ('Two rows of 10 make…', '20', ['10', '12'])])
L('g1', 'add_intro', 'What is adding?', 'Put groups together and count them all',
  lambda: [sc_add(2, 3), sc_add(4, 2), sc_add(3, 5), sc_add(6, 4), sc_add(5, 5)],
  ['Adding puts groups together', '+ means plus, = means makes'],
  [('2 + 3 = ?', '5', ['4', '6']), ('4 + 4 = ?', '8', ['7', '9']), ('6 + 1 = ?', '7', ['5', '8'])])
L('g1', 'sub_intro', 'What is subtracting?', 'Take some away and count what is left',
  lambda: [sc_sub(5, 2), sc_sub(8, 3), sc_sub(10, 4), sc_sub(7, 5), sc_sub(9, 6)],
  ['Subtracting takes away', '− means minus'],
  [('5 − 2 = ?', '3', ['2', '4']), ('9 − 4 = ?', '5', ['4', '6']), ('10 − 10 = ?', '0', ['1', '10'])])
L('g1', 'add_line', 'Adding on a number line', 'Start, then hop forward',
  lambda: [sc_line(3, 4), sc_line(8, 5), sc_line(11, 6), sc_line(9, 9)],
  ['Adding moves you to the right'],
  [('Start at 6, hop 3. Where are you?', '9', ['8', '10']), ('8 + 5 = ?', '13', ['12', '14'])])
L('g1', 'doubles', 'Doubles', '3 + 3, 4 + 4, 5 + 5 …',
  lambda: [sc_double(2), sc_double(3), sc_double(4), sc_double(5), sc_double(6), sc_double(8)],
  ['A double is a number added to itself'],
  [('6 + 6 = ?', '12', ['11', '13']), ('Double 7 is…', '14', ['12', '15']), ('9 + 9 = ?', '18', ['16', '19'])])
L('g1', 'make_ten', 'Make a ten', 'A clever way to add',
  lambda: [sc_maketen(8, 5), sc_maketen(9, 4), sc_maketen(7, 6), sc_maketen(8, 7)],
  ['Fill up to 10 first', 'Then add what is left'],
  [('9 + 6 = 10 + ?', '5', ['4', '6']), ('8 + 5 = ?', '13', ['12', '14']), ('7 + 6 = ?', '13', ['12', '14'])])
L('g1', 'shapes_2d', 'Flat shapes', 'Count the sides and corners',
  lambda: [sc_shape(3, 'Triangle', P), sc_shape(4, 'Square', B), sc_shape(5, 'Pentagon', G), sc_shape(6, 'Hexagon', AU), sc_shape(0, 'Circle', PU)],
  ['Sides are straight lines', 'Corners are where sides meet'],
  [('How many sides does a triangle have?', '3', ['4', '5']), ('A hexagon has how many corners?', '6', ['5', '8']), ('Which shape has no corners?', 'circle', ['square', 'triangle'])])
L('g1', 'clock_hours', "Telling the time: o'clock", 'The short hand points to the hour',
  lambda: [sc_clock(3, 0), sc_clock(7, 0), sc_clock(12, 0), sc_clock(9, 0), sc_clock(5, 0)],
  ["Long hand on 12 = o'clock"],
  [("Short hand on 4, long hand on 12. What time?", "4 o'clock", ["12 o'clock", "half past 4"]), ('Which hand shows the hour?', 'the short hand', ['the long hand', 'both'])])

# ===================================================================== GRADE 2
L('g2', 'skip_count', 'Skip counting', 'Count by 2s, 5s and 10s',
  lambda: [sc_skip(2, 10, P), sc_skip(5, 10, B), sc_skip(10, 10, G)],
  ['2s land on even numbers', '5s end in 5 or 0', '10s always end in 0'],
  [('2, 4, 6, 8, ?', '10', ['9', '12']), ('5, 10, 15, ?', '20', ['25', '16']), ('30, 40, 50, ?', '60', ['55', '70'])])
L('g2', 'place_tens', 'Tens and ones', 'What the digits in a number mean',
  lambda: [sc_place(23), sc_place(47), sc_place(60), sc_place(85), sc_place(39)],
  ['The left digit counts tens', 'The right digit counts ones'],
  [('How many tens in 47?', '4', ['7', '40']), ('5 tens and 3 ones is…', '53', ['35', '8']), ('The 9 in 92 means…', '90', ['9', '2'])])
L('g2', 'even_odd', 'Even and odd', 'Can everyone have a partner?',
  lambda: [sc_evenodd(6), sc_evenodd(7), sc_evenodd(10), sc_evenodd(13), sc_evenodd(12)],
  ['Even: everyone has a partner', 'Odd: one is left over'],
  [('Is 14 even or odd?', 'even', ['odd', 'neither']), ('Is 9 even or odd?', 'odd', ['even', 'neither']), ('Which is even?', '28', ['27', '31'])])
L('g2', 'compare', 'Bigger or smaller?', 'Using < and >',
  lambda: [sc_compare(3, 8), sc_compare(15, 9), sc_compare(42, 24), sc_compare(7, 7), sc_compare(61, 16)],
  ['The open side faces the bigger number', '= means both sides are the same'],
  [('34 ? 43', '<', ['>', '=']), ('90 ? 19', '>', ['<', '=']), ('25 ? 25', '=', ['<', '>'])])
L('g2', 'col_add', 'Column adding', 'Add the ones, then the tens',
  lambda: [sc_column(23, 45), sc_column(51, 36), sc_column(142, 235), sc_column(62, 17)],
  ['Line up ones under ones', 'Always start on the right'],
  [('23 + 45 = ?', '68', ['58', '78']), ('142 + 235 = ?', '377', ['367', '387'])])
L('g2', 'col_add_carry', 'Carrying', 'When a column makes 10 or more',
  lambda: [sc_column(38, 45), sc_column(67, 26), sc_column(276, 148)],
  ['10 ones become 1 ten', 'Carry it to the next column'],
  [('38 + 45 = ?', '83', ['73', '713']), ('67 + 26 = ?', '93', ['83', '813']), ('276 + 148 = ?', '424', ['414', '324'])])
L('g2', 'col_sub', 'Column subtracting', 'Ones first, then tens',
  lambda: [sc_column(68, 25, '-'), sc_column(97, 43, '-'), sc_column(586, 254, '-'), sc_column(79, 36, '-')],
  ['Top number minus bottom number', 'Start on the right'],
  [('68 − 25 = ?', '43', ['33', '53']), ('586 − 254 = ?', '332', ['322', '342'])])
L('g2', 'col_sub_borrow', 'Borrowing', 'When the top digit is too small',
  lambda: [sc_column(52, 27, '-'), sc_column(83, 46, '-'), sc_column(431, 182, '-')],
  ['Borrow 1 ten = 10 ones', 'Cross out and write the new digit'],
  [('52 − 27 = ?', '25', ['35', '24']), ('83 − 46 = ?', '37', ['43', '47']), ('431 − 182 = ?', '249', ['259', '351'])])
L('g2', 'fact_family', 'Fact families', 'One triangle, four facts',
  lambda: [sc_family(3, 5), sc_family(6, 7), sc_family(4, 9), sc_family(8, 5)],
  ['Adding and subtracting undo each other'],
  [('If 6 + 7 = 13, then 13 − 7 = ?', '6', ['7', '13']), ('If 4 + 9 = 13, then 13 − 4 = ?', '9', ['4', '5'])])
L('g2', 'clock_half', 'Half past and quarter past', '30 minutes and 15 minutes',
  lambda: [sc_clock(2, 30), sc_clock(5, 15), sc_clock(8, 45), sc_clock(11, 30), sc_clock(4, 15)],
  ['Half past: long hand on 6', 'Quarter past: long hand on 3'],
  [('Long hand on 6 means…', 'half past', ["o'clock", 'quarter past']), ('3:15 is…', 'quarter past 3', ['quarter to 3', 'half past 3'])])
L('g2', 'word_addsub', 'Story problems', 'Turn words into a number sentence',
  lambda: [W_(['Mia has 6 apples.', 'Her friend gives her 5 more.', 'How many apples now?'], [('6', P), '+', ('5', B), '=', ('11', AU)], '11 apples', 11, P),
           W_(['There are 14 birds on a fence.', '6 fly away.', 'How many are left?'], [('14', B), M, ('6', R), '=', ('8', AU)], '8 birds', 8, B),
           W_(['Sam read 23 pages on Monday', 'and 35 pages on Tuesday.', 'How many pages in all?'], [('23', P), '+', ('35', B), '=', ('58', AU)], '58 pages')],
  ['"more" / "in all": add', '"left" / "fly away": subtract'],
  [('Ali has 9 cards and gets 4 more. How many?', '13', ['5', '12']), ('20 kids, 8 go home. How many stay?', '12', ['28', '11'])])

# ===================================================================== GRADE 3
L('g3', 'mul_groups', 'What is multiplying?', 'Equal groups',
  lambda: [sc_groups(3, 2), sc_groups(4, 3), sc_groups(2, 5), sc_groups(5, 4)],
  ['× means groups of', 'It is quick adding'],
  [('3 groups of 4 = ?', '12', ['7', '10']), ('5 × 2 = ?', '10', ['7', '12'])])
L('g3', 'mul_arrays', 'Arrays', 'Rows and columns',
  lambda: [sc_array(3, 4), sc_array(2, 6), sc_array(5, 5), sc_array(4, 7)],
  ['rows × columns', 'You can swap the order'],
  [('4 rows of 7 = ?', '28', ['24', '11']), ('If 3 × 8 = 24, then 8 × 3 = ?', '24', ['11', '38'])])
L('g3', 'times_2_5_10', 'The 2, 5 and 10 times tables', 'The easiest three',
  lambda: [title_card('Times 2 = double', '2 × 7 = 7 + 7 = 14', B, 4.0), sc_table(2), sc_table(5), sc_table(10)],
  ['Times 2 = double', '5s end in 5 or 0', '10s: put a 0 on the end'],
  [('2 × 9 = ?', '18', ['16', '11']), ('5 × 7 = ?', '35', ['30', '12']), ('10 × 6 = ?', '60', ['16', '66'])])
L('g3', 'times_3_4', 'The 3 and 4 times tables', 'Count by 3s, double-double for 4s',
  lambda: [sc_table(3), title_card('Times 4 = double, double', '4 × 6 → 12 → 24', B, 4.0), sc_table(4), sc_array(4, 6)],
  ['3, 6, 9, 12, 15 …', 'Times 4 = double then double again'],
  [('3 × 8 = ?', '24', ['21', '27']), ('4 × 7 = ?', '28', ['24', '32'])])
L('g3', 'div_share', 'What is dividing?', 'Share fairly',
  lambda: [sc_share(6, 2), sc_share(12, 3), sc_share(15, 5), sc_share(20, 4)],
  ['÷ means share equally', 'Every group gets the same'],
  [('12 ÷ 3 = ?', '4', ['3', '9']), ('20 shared by 4 = ?', '5', ['4', '16'])])
L('g3', 'div_groups', 'Dividing into groups', 'How many groups can you make?',
  lambda: [sc_divgroups(12, 4), sc_divgroups(18, 6), sc_divgroups(20, 5), sc_divgroups(16, 4)],
  ['Dividing undoes multiplying'],
  [('How many groups of 6 in 18?', '3', ['2', '6']), ('If 5 × 4 = 20, then 20 ÷ 5 = ?', '4', ['5', '15'])])
L('g3', 'fractions_intro', 'What is a fraction?', 'Equal parts of a whole',
  lambda: [sc_pie(1, 2, 'one half'), sc_pie(1, 4, 'one quarter'), sc_pie(3, 4, 'three quarters'), sc_pie(2, 3, 'two thirds')],
  ['Top: parts you have', 'Bottom: equal parts in total'],
  [('A pizza in 4 equal slices, you eat 1. Fraction eaten?', '1/4', ['1/3', '4/1']), ('In 3/5, how many equal parts in total?', '5', ['3', '8'])])
L('g3', 'perimeter', 'Perimeter', 'The distance all the way around',
  lambda: [sc_perimeter(6, 4), sc_perimeter(9, 3), sc_perimeter(5, 5), sc_perimeter(8, 2)],
  ['Add every side', 'Rectangle: 2 × (long + short)'],
  [('Perimeter of a 6 by 4 rectangle?', '20', ['24', '10']), ('A square with sides of 5. Perimeter?', '20', ['25', '10'])])
L('g3', 'round_10', 'Rounding to the nearest 10', 'Which ten is closer?',
  lambda: [sc_round(23, 10), sc_round(47, 10), sc_round(65, 10), sc_round(81, 10), sc_round(240, 100)],
  ['5 or more rounds up', '4 or less rounds down'],
  [('Round 36 to the nearest 10', '40', ['30', '35']), ('Round 72 to the nearest 10', '70', ['80', '75']), ('Round 350 to the nearest 100', '400', ['300', '350'])])
L('g3', 'place_hundreds', 'Hundreds, tens and ones', 'Three-digit numbers',
  lambda: [sc_place(124), sc_place(305), sc_place(472), sc_place(860)],
  ['347 = 300 + 40 + 7', 'A 0 holds an empty place'],
  [('What is the 6 worth in 652?', '600', ['60', '6']), ('4 hundreds, 0 tens, 9 ones', '409', ['49', '490'])])
L('g3', 'clock_minutes', 'Reading minutes', 'Count the long hand in 5s',
  lambda: [sc_clock(4, 10), sc_clock(6, 25), sc_clock(1, 40), sc_clock(10, 55), sc_clock(3, 35)],
  ['Each number on the clock is 5 minutes'],
  [('Long hand on 4 means how many minutes?', '20', ['4', '40']), ('Long hand on 9 means…', '45 minutes', ['9 minutes', '30 minutes'])])

# ===================================================================== GRADE 4
L('g4', 'times_6_7', 'The 6 and 7 times tables', 'Use facts you already know',
  lambda: [title_card('6 × n = 5 × n + n', '6 × 7 = 35 + 7 = 42', P, 4.0), sc_table(6), sc_table(7)],
  ['6 × 7 = 42', '7 × 8 = 56'],
  [('6 × 8 = ?', '48', ['42', '54']), ('7 × 7 = ?', '49', ['42', '56']), ('7 × 9 = ?', '63', ['56', '72'])])
L('g4', 'times_8_9', 'The 8 and 9 times tables', 'Double-double-double, and the 9s trick',
  lambda: [sc_table(8), title_card('9s: digits add up to 9', '9 × 6 = 54  →  5 + 4 = 9', P, 4.0), sc_table(9)],
  ['8 × 8 = 64', '9 × n = 10 × n − n'],
  [('8 × 7 = ?', '56', ['54', '64']), ('9 × 8 = ?', '72', ['81', '63'])])
L('g4', 'mul_column', 'Column multiplying', 'Big number × small number',
  lambda: [sc_mulcol(23, 3), sc_mulcol(47, 6), sc_mulcol(126, 4), sc_mulcol(58, 7)],
  ['Multiply each digit, right to left', 'Add the carried number'],
  [('23 × 3 = ?', '69', ['66', '96']), ('47 × 6 = ?', '282', ['242', '272'])])
L('g4', 'div_remainder', 'Remainders', 'When it does not share evenly',
  lambda: [sc_share(7, 2), sc_share(14, 4), sc_share(17, 5), sc_share(11, 3)],
  ['The remainder is what is left over', 'It must be smaller than the groups'],
  [('17 ÷ 5 = ?', '3 r 2', ['3 r 1', '4 r 2']), ('14 ÷ 4 = ?', '3 r 2', ['2 r 6', '3 r 1'])])
L('g4', 'frac_equivalent', 'Equivalent fractions', 'Same amount, different numbers',
  lambda: [sc_fracbars([(1, 2), (2, 4), (4, 8)], '1/2 = 2/4 = 4/8'), sc_fracbars([(1, 3), (2, 6)], '1/3 = 2/6'),
           sc_steps(['2/3', '= (2 × 2)/(3 × 2)', '= 4/6'], ['', 'times top AND bottom', 'same amount!'])],
  ['Multiply (or divide) top and bottom by the same number'],
  [('1/2 = ?/8', '4', ['2', '6']), ('Which equals 3/4?', '6/8', ['3/8', '4/3'])])
L('g4', 'frac_compare', 'Comparing fractions', 'Which piece is bigger?',
  lambda: [sc_fracbars([(1, 2), (1, 3), (1, 4)], 'more parts → smaller pieces'), sc_fracbars([(2, 5), (4, 5)], '4/5 > 2/5'),
           sc_steps(['2/3  ?  3/4', '8/12  ?  9/12', '8/12 < 9/12', 'so 2/3 < 3/4'], ['', 'same bottom: 12', 'compare tops', ''])],
  ['Same bottom: compare the tops', 'Different bottoms: make them the same'],
  [('Which is bigger: 1/3 or 1/5?', '1/3', ['1/5', 'they are equal']), ('Which is bigger: 3/7 or 5/7?', '5/7', ['3/7', 'they are equal'])])
L('g4', 'frac_add_same', 'Adding fractions', 'Same bottom: add the tops',
  lambda: [sc_fracbars([(1, 5), (2, 5), (3, 5)], '1/5 + 2/5 = 3/5'), sc_steps(['3/8 + 4/8', '= (3 + 4)/8', '= 7/8'], ['', 'keep the bottom', '']),
           sc_steps(['5/6 − 1/6', '= 4/6', '= 2/3'], ['', 'subtract the tops', 'simplify'])],
  ['Add the tops, keep the bottom', 'Never add the bottoms'],
  [('2/7 + 3/7 = ?', '5/7', ['5/14', '6/7']), ('7/9 − 4/9 = ?', '3/9', ['3/0', '11/9'])])
L('g4', 'decimals', 'Decimals: tenths and hundredths', 'Numbers between whole numbers',
  lambda: [sc_percent(30), sc_steps(['0.3 = 3 tenths = 3/10', '0.07 = 7 hundredths = 7/100', '0.45 = 45/100', '2.5 = 2 + 5/10'], ['', '', '4 tenths + 5 hundredths', 'two and a half']),
           sc_steps(['0.5  ?  0.45', '0.50  ?  0.45', '0.50 > 0.45'], ['', 'add a zero to compare', 'bigger!'])],
  ['The first place after the point is tenths', 'The second is hundredths'],
  [('0.7 as a fraction?', '7/10', ['7/100', '1/7']), ('Which is bigger: 0.6 or 0.58?', '0.6', ['0.58', 'equal'])])
L('g4', 'angles', 'Angles and degrees', 'Acute, right, obtuse, straight',
  lambda: [sc_angle(40), sc_angle(90), sc_angle(130), sc_angle(180)],
  ['A full turn is 360°', 'A right angle is 90°'],
  [('An angle of 120° is…', 'obtuse', ['acute', 'right']), ('How many degrees in a right angle?', '90', ['45', '180'])])
L('g4', 'area_rect', 'Area of rectangles', 'Count the squares inside',
  lambda: [sc_area_rect(4, 3), sc_area_rect(6, 5), sc_area_rect(8, 2), sc_area_rect(7, 4)],
  ['Area = length × width', 'Measured in square units'],
  [('Area of a 6 by 5 rectangle?', '30', ['11', '22']), ('Area of a square with side 4?', '16', ['8', '12'])])
L('g4', 'factors', 'Factors and multiples', 'What goes into a number',
  lambda: [sc_array(3, 4), sc_array(2, 6), sc_steps(['Factors of 12:', '1 × 12', '2 × 6', '3 × 4', '1, 2, 3, 4, 6, 12'], ['', '', '', '', 'all of them']),
           sc_steps(['Multiples of 4:', '4, 8, 12, 16, 20 …'], ['', 'the 4 times table'])],
  ['Factors divide in exactly', 'Multiples are the times table'],
  [('Is 5 a factor of 20?', 'yes', ['no', 'only sometimes']), ('Which is a multiple of 6?', '18', ['15', '20'])])

# ===================================================================== GRADE 5
L('g5', 'frac_add_diff', 'Adding fractions with different bottoms', 'Find a common denominator',
  lambda: [sc_steps(['1/2 + 1/3', '= 3/6 + 2/6', '= 5/6'], ['different sizes!', 'common bottom: 6', '']),
           sc_steps(['3/4 + 1/6', '= 9/12 + 2/12', '= 11/12'], ['', 'common bottom: 12', '']),
           sc_steps(['5/6 − 1/4', '= 10/12 − 3/12', '= 7/12'], ['', '', ''])],
  ['Make the bottoms the same first', 'Then add or subtract the tops'],
  [('1/2 + 1/4 = ?', '3/4', ['2/6', '1/6']), ('2/3 − 1/6 = ?', '1/2', ['1/3', '1/6'])])
L('g5', 'frac_multiply', 'Multiplying fractions', 'Tops times tops, bottoms times bottoms',
  lambda: [sc_steps(['2/3 × 3/4', '= (2 × 3)/(3 × 4)', '= 6/12', '= 1/2'], ['', '', '', 'simplify']),
           sc_steps(['1/2 of 3/5', '= 1/2 × 3/5', '= 3/10'], ['"of" means ×', '', '']),
           sc_steps(['3 × 2/5', '= 3/1 × 2/5', '= 6/5 = 1 1/5'], ['', 'a whole number is n/1', ''])],
  ['No common bottom needed', '"of" means multiply'],
  [('1/2 × 1/3 = ?', '1/6', ['2/5', '1/5']), ('2/5 × 5/6 = ?', '1/3', ['7/11', '10/11'])])
L('g5', 'dec_addsub', 'Adding and subtracting decimals', 'Line up the decimal points',
  lambda: [sc_steps(['  3.45', '+ 1.20', '= 4.65'], ['', 'points in a line', '']),
           sc_steps(['  7.3', '− 2.85', '  7.30 − 2.85', '= 4.45'], ['', '', 'fill a zero', '']),
           sc_steps(['0.1 + 0.2', '= 0.3'], ['1 tenth + 2 tenths', ''])],
  ['Line up the points', 'Fill gaps with zeros'],
  [('2.5 + 1.25 = ?', '3.75', ['3.30', '2.75']), ('5 − 1.4 = ?', '3.6', ['4.6', '3.4'])])
L('g5', 'dec_multiply', 'Multiplying decimals', 'Multiply, then count decimal places',
  lambda: [sc_steps(['0.3 × 4', '3 × 4 = 12', '1 decimal place → 1.2'], ['', 'ignore the point', '']),
           sc_steps(['1.5 × 0.2', '15 × 2 = 30', '2 decimal places → 0.30 = 0.3'], ['', '', '']),
           sc_steps(['2.4 × 10 = 24', '2.4 × 100 = 240'], ['point moves 1 right', 'point moves 2 right'])],
  ['Count the decimal places in both numbers', '× 10 moves the point one place right'],
  [('0.4 × 3 = ?', '1.2', ['12', '0.12']), ('3.6 × 10 = ?', '36', ['3.60', '360'])])
L('g5', 'volume', 'Volume of boxes', 'How many cubes fit inside?',
  lambda: [sc_box(3, 2, 2), sc_box(4, 3, 2), sc_box(2, 2, 3)],
  ['Volume = length × width × height', 'Measured in cubic units'],
  [('Volume of a 4 × 3 × 2 box?', '24', ['9', '14']), ('A cube with side 3. Volume?', '27', ['9', '18'])])
L('g5', 'order_ops', 'Order of operations', 'Brackets, powers, × ÷, + −',
  lambda: [sc_steps(['3 + 4 × 2', '= 3 + 8', '= 11'], ['', '× before +', 'not 14!']),
           sc_steps(['(3 + 4) × 2', '= 7 × 2', '= 14'], ['', 'brackets first', '']),
           sc_steps(['20 − 2 × 3²', '= 20 − 2 × 9', '= 20 − 18', '= 2'], ['', 'powers', 'multiply', 'subtract'])],
  ['Brackets → powers → × ÷ → + −', 'Left to right inside each step'],
  [('2 + 3 × 4 = ?', '14', ['20', '24']), ('(8 − 2) ÷ 3 = ?', '2', ['6', '7.3'])])
L('g5', 'coord_plane', 'The coordinate plane', 'Find points with (x, y)',
  lambda: [sc_graph([], (0, 10), (0, 8), notes=['x: go across', 'y: go up', '(x, y)'], marks=[(2, 3, '(2, 3)', P), (5, 1, '(5, 1)', B), (7, 6, '(7, 6)', G), (0, 4, '(0, 4)', AU)], step=(1, 1)),
           sc_graph([], (0, 10), (0, 8), notes=['A square:', '(2,2) (6,2)', '(6,6) (2,6)'], marks=[(2, 2, 'A', P), (6, 2, 'B', P), (6, 6, 'C', P), (2, 6, 'D', P)], step=(1, 1))],
  ['Across first, then up', '(0, 0) is the origin'],
  [('In (4, 7), how far up?', '7', ['4', '11']), ('Which point is on the x-axis?', '(5, 0)', ['(0, 5)', '(5, 5)'])])

# ===================================================================== GRADE 6
L('g6', 'ratios', 'Ratios', 'Comparing two amounts',
  lambda: [sc_ratio(2, 3), sc_ratio(1, 4), sc_steps(['2 : 3 = ? : 12', '× 4', '8 : 12'], ['', '3 × 4 = 12', 'do the same to both'])],
  ['A ratio compares amounts', 'Scale both sides by the same number'],
  [('Simplify 6 : 9', '2 : 3', ['3 : 2', '6 : 3']), ('2 : 5 = 8 : ?', '20', ['11', '16'])])
L('g6', 'percent_intro', 'What is a percent?', 'Out of 100',
  lambda: [sc_percent(25), sc_percent(60), sc_steps(['50% = 50/100 = 1/2', '25% = 1/4', '10% = 1/10 = 0.1'], ['', '', ''])],
  ['Percent means "out of 100"', '50% is half'],
  [('75% as a fraction?', '3/4', ['7/5', '1/75']), ('0.4 as a percent?', '40%', ['4%', '0.4%'])])
L('g6', 'percent_of', 'Percent of a number', 'Find 20% of 60',
  lambda: [sc_percent(20, 60), sc_percent(15, 80), sc_steps(['10% of 250 = 25', '30% = 3 × 25 = 75'], ['divide by 10', 'build from 10%'])],
  ['p% of n = p/100 × n', 'Find 10% first, then build'],
  [('10% of 90 = ?', '9', ['90', '0.9']), ('25% of 40 = ?', '10', ['25', '15'])])
L('g6', 'negatives', 'Negative numbers', 'Numbers below zero',
  lambda: [sc_iline(3, 5, '-'), sc_iline(-4, 6, '+'), sc_iline(-2, 5, '-'), sc_steps(['−5 < −2', 'colder: −10 °C < −3 °C'], ['further left = smaller', ''])],
  ['Left of 0 is negative', 'Further left = smaller'],
  [('Which is smaller: −8 or −3?', '−8', ['−3', 'equal']), ('3 − 7 = ?', '−4', ['4', '−10'])])
L('g6', 'exponents', 'Exponents', 'Repeated multiplication',
  lambda: [sc_square(4), sc_steps(['2³ = 2 × 2 × 2', '= 8'], ['3 twos multiplied', '']), sc_steps(['10⁴ = 10 × 10 × 10 × 10', '= 10 000'], ['4 zeros', '']),
           sc_steps(['5² = 25', '3⁴ = 81', 'n¹ = n', 'n⁰ = 1'], ['', '', '', 'any non-zero n'])],
  ['The power says how many times to multiply', '2³ is 8, not 6'],
  [('3³ = ?', '27', ['9', '6']), ('10² = ?', '100', ['20', '1000'])])
L('g6', 'area_triangle', 'Area of triangles', 'Half a rectangle',
  lambda: [sc_area_rect(6, 4, True), sc_area_rect(8, 5, True), sc_steps(['base 10, height 7', 'A = ½ × 10 × 7', '= 35'], ['', '', 'square units'])],
  ['Area = ½ × base × height'],
  [('Triangle base 6, height 4. Area?', '12', ['24', '10']), ('Triangle base 9, height 2. Area?', '9', ['18', '11'])])
L('g6', 'mean_median', 'Mean, median and mode', 'Three kinds of "middle"',
  lambda: [sc_bars([3, 7, 5, 7, 8], ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'], title='Goals scored'), sc_bars([2, 4, 4, 6, 9], show=('mean', 'median'))],
  ['Mean: add up, divide by how many', 'Median: middle when sorted', 'Mode: most common'],
  [('Mean of 2, 4, 6?', '4', ['6', '12']), ('Median of 1, 3, 8, 9, 10?', '8', ['3', '6.2']), ('Mode of 2, 5, 5, 7?', '5', ['2', '7'])])
L('g6', 'gcf_lcm', 'GCF and LCM', 'Greatest common factor, least common multiple',
  lambda: [sc_steps(['12: 1, 2, 3, 4, 6, 12', '18: 1, 2, 3, 6, 9, 18', 'GCF = 6'], ['', '', 'biggest shared factor']),
           sc_steps(['4: 4, 8, 12, 16 …', '6: 6, 12, 18 …', 'LCM = 12'], ['', '', 'smallest shared multiple']),
           sc_steps(['GCF × LCM = 12 × 18', '6 × 36 = 216 ✓'], ['for two numbers', ''])],
  ['GCF: biggest number that divides both', 'LCM: smallest number both divide into'],
  [('GCF of 8 and 12?', '4', ['2', '24']), ('LCM of 3 and 5?', '15', ['8', '1'])])

# ===================================================================== GRADE 7
L('g7', 'integers', 'Adding and subtracting integers', 'Negatives on the number line',
  lambda: [sc_iline(-3, 5, '+'), sc_iline(2, -6, '+'), sc_iline(-1, -4, '-'), sc_steps(['5 − (−3)', '= 5 + 3', '= 8'], ['', 'minus a negative = plus', ''])],
  ['Adding a negative moves left', 'Subtracting a negative moves right'],
  [('−6 + 4 = ?', '−2', ['2', '−10']), ('3 − (−2) = ?', '5', ['1', '−5'])])
L('g7', 'eq_one_step', 'One-step equations', 'Undo to find x',
  lambda: [sc_steps(['x + 7 = 12', 'x + 7 − 7 = 12 − 7', 'x = 5'], ['', '− 7 on both sides', 'check: 5 + 7 = 12 ✓']),
           sc_steps(['4x = 28', '4x ÷ 4 = 28 ÷ 4', 'x = 7'], ['', '÷ 4 on both sides', '']),
           sc_steps(['x/3 = 6', 'x = 6 × 3', 'x = 18'], ['', '× 3 on both sides', ''])],
  ['Do the opposite operation', 'Do the same to both sides'],
  [('x − 4 = 10. x = ?', '14', ['6', '40']), ('5x = 35. x = ?', '7', ['30', '175'])])
L('g7', 'eq_two_step', 'Two-step equations', 'Undo + and −, then × and ÷',
  lambda: [sc_steps(['3x + 5 = 20', '3x = 15', 'x = 5'], ['', '− 5 both sides', '÷ 3 both sides']),
           sc_steps(['2x − 7 = 9', '2x = 16', 'x = 8'], ['', '+ 7', '÷ 2']),
           sc_steps(['x/4 + 1 = 3', 'x/4 = 2', 'x = 8'], ['', '− 1', '× 4'])],
  ['Undo adding first', 'Then undo multiplying'],
  [('2x + 3 = 11. x = ?', '4', ['7', '5.5']), ('5x − 5 = 20. x = ?', '5', ['3', '4'])])
L('g7', 'proportions', 'Proportions', 'Cross multiply',
  lambda: [sc_steps(['3/4 = x/20', '4 × x = 3 × 20', '4x = 60', 'x = 15'], ['', 'cross multiply', '', '']),
           sc_steps(['2 pens cost $3', '10 pens cost ?', '2/3 = 10/x', 'x = 15'], ['', '', '', '$15']),
           sc_ratio(3, 2, 2)],
  ['Two equal ratios make a proportion', 'Cross products are equal'],
  [('2/5 = x/15. x = ?', '6', ['3', '12']), ('3 apples cost $6. 5 apples?', '$10', ['$8', '$11'])])
L('g7', 'circles', 'Circles: circumference and area', 'Meet π',
  lambda: [sc_circle(3), sc_circle(5), sc_steps(['diameter d = 2r', 'C = πd'], ['across the middle', 'same as 2πr'])],
  ['C = 2πr', 'A = πr²', 'π ≈ 3.14'],
  [('Area of a circle with r = 2 (π ≈ 3.14)?', '12.56', ['6.28', '4']), ('Diameter if r = 6?', '12', ['3', '36'])])
L('g7', 'probability', 'Probability', 'How likely is it?',
  lambda: [sc_steps(['Coin: heads or tails', 'P(heads) = 1/2'], ['2 outcomes', '1 favourable']),
           sc_steps(['Die: 1 2 3 4 5 6', 'P(even) = 3/6 = 1/2'], ['', '2, 4, 6']), sc_dice(7), sc_dice(10)],
  ['P = favourable ÷ all outcomes', '0 = impossible, 1 = certain'],
  [('P(rolling a 6 on one die)?', '1/6', ['1/2', '6']), ('Most likely sum of two dice?', '7', ['12', '6'])])

# ===================================================================== GRADE 8
L('g8', 'pythagoras', 'Pythagoras', 'Right triangles: a² + b² = c²',
  lambda: [sc_pyth(3, 4), sc_pyth(5, 12), sc_steps(['a² + 6² = 10²', 'a² = 100 − 36 = 64', 'a = 8'], ['find a short side', '', ''])],
  ['Only for right triangles', 'c is the longest side (hypotenuse)'],
  [('Legs 6 and 8. Hypotenuse?', '10', ['14', '7']), ('Legs 5 and 12. Hypotenuse?', '13', ['17', '11'])])
L('g8', 'slope', 'Slope', 'Rise over run',
  lambda: [sc_graph([(lambda x: 2 * x + 1, B, 'y = 2x + 1')], (-1, 6), (-1, 12), notes=['up 2, right 1', 'slope = 2'], marks=[(1, 3, '(1, 3)', P), (3, 7, '(3, 7)', P)], step=(1, 1)),
           sc_steps(['points (1, 3) and (3, 7)', 'slope = (7 − 3)/(3 − 1)', '= 4/2 = 2'], ['', 'rise / run', '']),
           sc_graph([(lambda x: -0.5 * x + 4, R, 'y = −½x + 4')], (-1, 9), (-1, 6), notes=['going down', 'negative slope'], step=(1, 1))],
  ['slope = rise ÷ run', 'Up = positive, down = negative'],
  [('Slope through (0, 1) and (2, 5)?', '2', ['4', '1/2']), ('A flat line has slope…', '0', ['1', 'undefined'])])
L('g8', 'linear', 'Linear functions', 'y = mx + b',
  lambda: [sc_graph([(lambda x: x, B, 'y = x'), (lambda x: x + 3, G, 'y = x + 3'), (lambda x: 3 * x, P, 'y = 3x')], (-4, 4), (-6, 8), notes=['b moves it up', 'm makes it steeper'], step=(1, 1)),
           sc_steps(['y = 2x − 1', 'x = 0 → y = −1', 'x = 3 → y = 5'], ['m = 2, b = −1', 'the y-intercept', ''])],
  ['m = slope', 'b = where it crosses the y-axis'],
  [('Slope of y = 4x − 2?', '4', ['−2', '2']), ('y-intercept of y = 3x + 5?', '5', ['3', '8'])])
L('g8', 'systems', 'Systems of equations', 'Where two lines meet',
  lambda: [sc_graph([(lambda x: x + 1, B, 'y = x + 1'), (lambda x: -x + 5, P, 'y = −x + 5')], (-1, 7), (-1, 7), notes=['they cross at', '(2, 3)'], marks=[(2, 3, '(2, 3)', AU)], step=(1, 1)),
           sc_steps(['x + y = 10', 'x − y = 4', '2x = 14 → x = 7', 'y = 3'], ['', '', 'add the equations', 'put x back'])],
  ['The solution works in both equations', 'Graph it or add/substitute'],
  [('x + y = 5 and x − y = 1. x = ?', '3', ['2', '4']), ('Where do y = x and y = 4 meet?', '(4, 4)', ['(0, 4)', '(4, 0)'])])
L('g8', 'sci_notation', 'Scientific notation', 'Very big and very small numbers',
  lambda: [sc_steps(['300 000 000', '= 3 × 10⁸'], ['speed of light, m/s', 'move the point 8 places']),
           sc_steps(['0.000 52', '= 5.2 × 10⁻⁴'], ['', 'small → negative power']),
           sc_steps(['(2 × 10³) × (4 × 10⁵)', '= 8 × 10⁸'], ['', 'multiply, add powers'])],
  ['a × 10ⁿ with 1 ≤ a < 10', 'Big → positive n, small → negative n'],
  [('4500 in scientific notation?', '4.5 × 10³', ['45 × 10²', '4.5 × 10⁴']), ('2 × 10⁻² = ?', '0.02', ['0.2', '200'])])
L('g8', 'square_roots', 'Square roots', 'The number that squares to it',
  lambda: [sc_square(5), sc_square(7), sc_steps(['√50 is between 7 and 8', '7² = 49, 8² = 64', '√50 ≈ 7.07'], ['', '', ''])],
  ['√(n²) = n', 'Perfect squares: 1, 4, 9, 16, 25, 36 …'],
  [('√81 = ?', '9', ['8', '40.5']), ('√20 is between…', '4 and 5', ['3 and 4', '5 and 6'])])

# ===================================================================== GRADE 9
L('g9', 'inequalities', 'Solving inequalities', 'Like equations, with one twist',
  lambda: [sc_ineq(3, '>'), sc_ineq(-1, '<='), sc_steps(['2x + 1 < 9', '2x < 8', 'x < 4'], ['', '', '']),
           sc_steps(['−3x ≤ 12', 'x ≥ −4'], ['', '÷ negative → flip the sign!'])],
  ['Solve like an equation', 'Multiply/divide by a negative → flip the sign'],
  [('Solve x + 3 > 7', 'x > 4', ['x > 10', 'x < 4']), ('Solve −2x < 6', 'x > −3', ['x < −3', 'x > 3'])])
L('g9', 'polynomials', 'Multiplying brackets', 'Every term times every term',
  lambda: [sc_steps(['(x + 3)(x + 2)', '= x² + 2x + 3x + 6', '= x² + 5x + 6'], ['', 'first, outer, inner, last', 'combine like terms']),
           sc_steps(['(x − 4)(x + 4)', '= x² + 4x − 4x − 16', '= x² − 16'], ['', '', 'difference of squares']),
           sc_steps(['(x + 5)²', '= x² + 10x + 25'], ['', '2 × 5 = 10, 5² = 25'])],
  ['Multiply each term by each term', 'Then combine like terms'],
  [('(x + 1)(x + 4) = ?', 'x² + 5x + 4', ['x² + 4', 'x² + 4x + 5']), ('(x + 3)² = ?', 'x² + 6x + 9', ['x² + 9', 'x² + 3x + 9'])])
L('g9', 'factoring', 'Factoring', 'Brackets in reverse',
  lambda: [sc_steps(['x² + 7x + 12', 'two numbers: × 12, + 7', '3 and 4', '= (x + 3)(x + 4)'], ['', '', '3 × 4 = 12, 3 + 4 = 7', '']),
           sc_steps(['x² − x − 6', '−3 and 2', '= (x − 3)(x + 2)'], ['', '× = −6, + = −1', '']),
           sc_steps(['6x + 9', '= 3(2x + 3)'], ['', 'common factor 3'])],
  ['Find two numbers that multiply to c and add to b'],
  [('Factor x² + 5x + 6', '(x + 2)(x + 3)', ['(x + 1)(x + 6)', '(x + 5)(x + 1)']), ('Factor 4x + 8', '4(x + 2)', ['4(x + 8)', '2(x + 8)'])])
L('g9', 'quadratic_graph', 'Parabolas', 'The graph of a quadratic',
  lambda: [sc_graph([(lambda x: x * x, B, 'y = x²'), (lambda x: x * x - 4, P, 'y = x² − 4')], (-4, 4), (-5, 10), notes=['U-shape', 'roots: where y = 0'], marks=[(-2, 0, '−2', AU), (2, 0, '2', AU), (0, -4, 'vertex', G)], step=(1, 1)),
           sc_graph([(lambda x: -(x - 1) ** 2 + 4, G, 'y = −(x−1)² + 4')], (-3, 5), (-5, 6), notes=['minus → upside down', 'vertex (1, 4)'], marks=[(1, 4, '(1, 4)', AU)], step=(1, 1))],
  ['y = ax² + bx + c makes a U', 'a < 0 turns it upside down'],
  [('Roots of y = x² − 9?', '−3 and 3', ['9 only', '0 and 9']), ('Vertex of y = (x − 2)² + 1?', '(2, 1)', ['(−2, 1)', '(1, 2)'])])
L('g9', 'quadratic_formula', 'The quadratic formula', 'Solve any ax² + bx + c = 0',
  lambda: [sc_steps(['x = (−b ± √(b² − 4ac)) / 2a'], ['works every time']),
           sc_steps(['x² − 5x + 6 = 0', 'a = 1, b = −5, c = 6', 'x = (5 ± √(25 − 24)) / 2', 'x = (5 ± 1)/2', 'x = 3 or x = 2'], ['', '', '', '', '']),
           sc_steps(['b² − 4ac > 0: two answers', '= 0: one answer', '< 0: no real answers'], ['the discriminant', '', ''])],
  ['x = (−b ± √(b² − 4ac)) / 2a'],
  [('Solve x² − 4 = 0', 'x = ±2', ['x = 4', 'x = 2 only']), ('If b² − 4ac < 0, real answers?', 'none', ['one', 'two'])])
L('g9', 'exponent_rules', 'Exponent rules', 'Shortcuts for powers',
  lambda: [sc_steps(['x³ · x⁴ = x⁷', 'x⁸ ÷ x² = x⁶', '(x²)³ = x⁶'], ['multiply → add powers', 'divide → subtract', 'power of power → multiply']),
           sc_steps(['x⁰ = 1', 'x⁻² = 1/x²', 'x^(1/2) = √x'], ['', 'negative → flip', 'fraction → root'])],
  ['Same base: add powers when multiplying', 'Subtract when dividing'],
  [('2³ · 2² = 2^?', '5', ['6', '1']), ('5⁻¹ = ?', '1/5', ['−5', '0'])])

# ===================================================================== GRADE 10
L('g10', 'triangle_angles', 'Angles in a triangle', 'They always add up to 180°',
  lambda: [sc_steps(['50° + 60° + ? = 180°', '? = 180° − 110°', '? = 70°'], ['', '', '']), sc_angle(180),
           sc_steps(['isosceles: two equal angles', 'top 40° → (180 − 40)/2 = 70° each'], ['', ''])],
  ['Angles in a triangle add to 180°', 'In a quadrilateral: 360°'],
  [('Two angles are 90° and 35°. The third?', '55°', ['65°', '125°']), ('Each angle of an equilateral triangle?', '60°', ['90°', '45°'])])
L('g10', 'similar', 'Similar triangles', 'Same shape, different size',
  lambda: [sc_steps(['sides 3, 4, 5', 'sides 6, 8, 10', 'scale factor 2'], ['small', 'big', 'every side × 2']),
           sc_steps(['4/6 = x/9', 'x = 6'], ['matching sides', 'cross multiply']),
           sc_steps(['area scales by k²', 'k = 2 → area × 4'], ['', ''])],
  ['Matching angles are equal', 'Matching sides share one scale factor'],
  [('Scale factor 3, a side of 5 becomes?', '15', ['8', '5/3']), ('Lengths × 2: area × ?', '4', ['2', '8'])])
L('g10', 'trig', 'Trigonometry: SOH CAH TOA', 'Sides and angles of right triangles',
  lambda: [sc_trig(3, 4), sc_trig(5, 12), sc_steps(['sin 30° = 0.5', 'hyp = 10 → opp = 10 × 0.5 = 5'], ['', 'find a side'])],
  ['sin = opp/hyp', 'cos = adj/hyp', 'tan = opp/adj'],
  [('Opposite 6, hypotenuse 10. sin θ = ?', '0.6', ['0.8', '1.67']), ('tan θ = opposite ÷ ?', 'adjacent', ['hypotenuse', 'opposite'])])
L('g10', 'volume_solids', 'Cylinders, cones and spheres', 'Volume formulas',
  lambda: [sc_steps(['cylinder: V = πr²h', 'r = 3, h = 10', 'V = π × 9 × 10 ≈ 282.7'], ['circle area × height', '', '']),
           sc_steps(['cone: V = ⅓πr²h', 'r = 3, h = 10 → V ≈ 94.2'], ['a third of the cylinder', '']),
           sc_steps(['sphere: V = 4/3 πr³', 'r = 3 → V ≈ 113.1'], ['', ''])],
  ['Cylinder: πr²h', 'Cone: ⅓πr²h', 'Sphere: 4/3 πr³'],
  [('Cone vs cylinder, same r and h: the cone is…', '1/3 of it', ['1/2 of it', 'the same']), ('Volume of a cylinder r = 1, h = 5 (in π)?', '5π', ['10π', 'π'])])
L('g10', 'distance_mid', 'Distance and midpoint', 'Pythagoras on a grid',
  lambda: [sc_graph([], (0, 9), (0, 7), notes=['A(1, 1), B(7, 5)', 'run 6, rise 4', 'd = √(6² + 4²) ≈ 7.21'], marks=[(1, 1, 'A', P), (7, 5, 'B', P), (4, 3, 'midpoint (4, 3)', G)], step=(1, 1)),
           sc_steps(['d = √((x₂ − x₁)² + (y₂ − y₁)²)', 'M = ((x₁ + x₂)/2, (y₁ + y₂)/2)'], ['distance', 'average the coordinates'])],
  ['Distance comes from Pythagoras', 'Midpoint = average of the points'],
  [('Midpoint of (0, 0) and (6, 4)?', '(3, 2)', ['(6, 4)', '(2, 3)']), ('Distance from (0, 0) to (3, 4)?', '5', ['7', '25'])])

# ===================================================================== GRADE 11
L('g11', 'functions', 'Functions and inverses', 'Input → rule → output',
  lambda: [sc_steps(['f(x) = 2x + 3', 'f(4) = 2·4 + 3 = 11'], ['the rule', 'put 4 in']),
           sc_steps(['y = 2x + 3', 'x = (y − 3)/2', 'f⁻¹(x) = (x − 3)/2'], ['', 'solve for x', 'the inverse undoes f']),
           sc_graph([(lambda x: 2 * x + 3, B, 'f(x)'), (lambda x: (x - 3) / 2, P, 'f⁻¹(x)'), (lambda x: x, D, 'y = x')], (-6, 8), (-6, 8), notes=['mirror images', 'in the line y = x'], step=(2, 2))],
  ['f(x) means "f of x"', 'The inverse reflects the graph in y = x'],
  [('f(x) = x² + 1. f(3) = ?', '10', ['7', '9']), ('Inverse of f(x) = x + 5?', 'x − 5', ['x + 5', '5x'])])
L('g11', 'exponential', 'Exponential growth', 'Multiply by the same amount each step',
  lambda: [sc_seq([1, 2, 4, 8, 16, 32], ['×2'] * 5, 'Doubling'), sc_graph([(lambda x: 2 ** x, G, 'y = 2ˣ'), (lambda x: 0.5 ** x, R, 'y = (½)ˣ')], (-4, 4), (-1, 12), notes=['growth', 'decay'], step=(1, 2)),
           sc_steps(['$100 at 10% a year', 'after 3 years: 100 × 1.1³', '≈ $133.10'], ['', '', 'compound interest'])],
  ['y = a · bˣ', 'b > 1 grows, 0 < b < 1 decays'],
  [('Bacteria double every hour from 5. After 3 hours?', '40', ['15', '30']), ('Which is decay?', 'y = 0.8ˣ', ['y = 3ˣ', 'y = 1.2ˣ'])])
L('g11', 'logs', 'Logarithms', 'The power you need',
  lambda: [sc_steps(['2³ = 8', 'log₂ 8 = 3'], ['', '"2 to what power is 8?"']),
           sc_steps(['log₁₀ 1000 = 3', 'log a + log b = log(ab)', 'log(aⁿ) = n log a'], ['', '', '']),
           sc_graph([(lambda x: 2 ** x, G, 'y = 2ˣ'), (lambda x: math.log2(x) if x > 0 else None, P, 'y = log₂ x'), (lambda x: x, D, 'y = x')], (-3, 8), (-3, 8), notes=['logs undo powers'], step=(1, 1))],
  ['log_b(x) = the power of b that gives x', 'Logs and exponents are inverses'],
  [('log₂ 32 = ?', '5', ['16', '6']), ('log₁₀ 0.01 = ?', '−2', ['2', '0.1'])])
L('g11', 'sequences', 'Arithmetic and geometric sequences', 'Patterns with rules',
  lambda: [sc_seq([3, 7, 11, 15, 19], ['+4'] * 4, 'Arithmetic: add the same'), sc_seq([2, 6, 18, 54, 162], ['×3'] * 4, 'Geometric: multiply the same'),
           sc_steps(['aₙ = a₁ + (n − 1)d', 'a₁₀ = 3 + 9 × 4 = 39'], ['arithmetic', '']), sc_steps(['aₙ = a₁ · rⁿ⁻¹', 'a₆ = 2 × 3⁵ = 486'], ['geometric', ''])],
  ['Arithmetic: + d', 'Geometric: × r'],
  [('Next: 5, 9, 13, ?', '17', ['18', '21']), ('Next: 3, 6, 12, ?', '24', ['15', '18'])])
L('g11', 'unit_circle', 'The unit circle', 'Where sine and cosine come from',
  lambda: [sc_unitcircle(), sc_steps(['sin 90° = 1, cos 90° = 0', 'sin² θ + cos² θ = 1'], ['top of the circle', 'Pythagoras!'])],
  ['cos θ = x, sin θ = y on the unit circle', '360° = 2π radians'],
  [('sin 0° = ?', '0', ['1', '−1']), ('180° in radians?', 'π', ['2π', 'π/2'])])
L('g11', 'complex', 'Complex numbers', 'When you need √−1',
  lambda: [sc_steps(['i = √−1', 'i² = −1'], ['a new number', '']),
           sc_steps(['(2 + 3i) + (1 − i)', '= 3 + 2i'], ['add real and imaginary parts', '']),
           sc_steps(['(1 + i)(1 − i)', '= 1 − i + i − i²', '= 1 + 1 = 2'], ['', '', 'i² = −1']),
           sc_graph([], (-2, 5), (-2, 5), notes=['3 + 2i', 'is the point (3, 2)'], marks=[(3, 2, '3 + 2i', AU)], step=(1, 1))],
  ['i² = −1', 'a + bi lives on a plane'],
  [('i² = ?', '−1', ['1', 'i']), ('(3 + i) + (2 + 4i) = ?', '5 + 5i', ['6 + 4i', '5 + 4i'])])

# ===================================================================== GRADE 12
L('g12', 'limits', 'Limits', 'Where a function is heading',
  lambda: [sc_graph([(lambda x: (x * x - 1) / (x - 1) if abs(x - 1) > 1e-6 else None, B, 'y = (x² − 1)/(x − 1)')], (-2, 4), (-1, 5), notes=['undefined at x = 1', 'but it heads to 2'], marks=[(1, 2, 'hole (1, 2)', AU, 'open')], step=(1, 1)),
           sc_steps(['lim x→1 (x² − 1)/(x − 1)', '= lim (x − 1)(x + 1)/(x − 1)', '= lim (x + 1)', '= 2'], ['', 'factor', 'cancel', '']),
           sc_graph([(lambda x: 1 / x if x > 0.02 else None, P, 'y = 1/x')], (0, 10), (0, 5), notes=['as x → ∞', '1/x → 0'], step=(1, 1))],
  ['A limit is the value you approach', 'It can exist even where f is undefined'],
  [('lim x→2 of (x + 3)?', '5', ['2', '3']), ('lim x→∞ of 1/x?', '0', ['1', '∞'])])
L('g12', 'derivative', 'The derivative', 'The slope at a point',
  lambda: [sc_tangent(lambda x: x * x / 2, lambda x: x, (-4, 4), (-1, 8), -3, 3, 'slope'),
           sc_steps(["f'(x) = lim h→0 (f(x+h) − f(x))/h", 'f(x) = x²', "f'(x) = 2x"], ['slope of a tiny step', '', ''])],
  ["f'(x) = slope of the tangent", 'Speed is the derivative of distance'],
  [('Slope of a horizontal tangent?', '0', ['1', 'undefined']), ("If f(x) = x², f'(3) = ?", '6', ['9', '3'])])
L('g12', 'power_rule', 'The power rule', 'd/dx of xⁿ',
  lambda: [sc_steps(['d/dx xⁿ = n xⁿ⁻¹'], ['bring the power down, lower it by 1']),
           sc_steps(['d/dx x³ = 3x²', 'd/dx 5x⁴ = 20x³', 'd/dx 7x = 7', 'd/dx 9 = 0'], ['', '', '', 'constants vanish']),
           sc_tangent(lambda x: x ** 3 / 4 - x, lambda x: 0.75 * x * x - 1, (-3, 3), (-4, 4), -2.5, 2.5, "f'(x)")],
  ['d/dx xⁿ = n xⁿ⁻¹', 'The derivative of a constant is 0'],
  [('d/dx x⁵ = ?', '5x⁴', ['x⁴', '5x⁵']), ('d/dx (3x² + 2x) = ?', '6x + 2', ['3x + 2', '6x²'])])
L('g12', 'integral', 'The integral', 'Area under a curve',
  lambda: [sc_riemann(lambda x: x * x, 0, 3, (-0.5, 3.5), (-1, 10), exact='∫₀³ x² dx = 9'),
           sc_steps(['∫ xⁿ dx = xⁿ⁺¹/(n + 1) + C', '∫₀³ x² dx = [x³/3]₀³', '= 27/3 − 0 = 9'], ['reverse of the derivative', '', ''])],
  ['∫ = add up thin slices', 'Integrating undoes differentiating'],
  [('∫ 2x dx = ?', 'x² + C', ['2 + C', 'x + C']), ('∫₀¹ 1 dx = ?', '1', ['0', 'x'])])
L('g12', 'trig_graphs', 'Graphs of sine and cosine', 'Waves that repeat',
  lambda: [sc_graph([(math.sin, P, 'y = sin x'), (math.cos, B, 'y = cos x')], (-1, 7), (-2, 2), notes=['period 2π ≈ 6.28', 'height 1'], step=(1, 0.5)),
           sc_graph([(lambda x: 2 * math.sin(2 * x), G, 'y = 2 sin 2x')], (-1, 7), (-3, 3), notes=['2 in front: taller', '2x inside: faster'], step=(1, 1))],
  ['y = A sin(Bx): A = height, period = 2π/B'],
  [('Max value of y = 3 sin x?', '3', ['1', '6']), ('Period of y = sin x?', '2π', ['π', '1'])])
L('g12', 'combinations', 'Counting: permutations and combinations', 'How many ways?',
  lambda: [sc_steps(['5! = 5 × 4 × 3 × 2 × 1', '= 120'], ['ways to line up 5 people', '']),
           sc_steps(['choose 2 of 5, order matters', '5 × 4 = 20'], ['permutation', '']),
           sc_steps(['choose 2 of 5, order does not matter', 'C(5,2) = 5!/(2! 3!)', '= 10'], ['combination', '', ''])],
  ['n! = n × (n−1) × … × 1', 'C(n,r) = n! / (r!(n−r)!)'],
  [('4! = ?', '24', ['16', '10']), ('C(4, 2) = ?', '6', ['8', '12'])])
L('g12', 'std_dev', 'Standard deviation', 'How spread out is the data?',
  lambda: [sc_bars([4, 5, 5, 6], show=('mean',), title='close together'), sc_bars([1, 3, 7, 9], show=('mean',), title='spread out'),
           sc_steps(['data 2, 4, 6 (mean 4)', 'distances² : 4, 0, 4', 'average: 8/3', 'σ = √(8/3) ≈ 1.63'], ['', '', '', ''])],
  ['Small σ: data close to the mean', 'Big σ: data spread out'],
  [('Which data has the bigger spread?', '1, 10, 20', ['9, 10, 11', '10, 10, 10']), ('σ of 5, 5, 5?', '0', ['5', '1'])])

# ===================================================================== COLLEGE
L('college', 'chain_rule', 'The chain rule', 'Derivatives of functions inside functions',
  lambda: [sc_steps(["(f(g(x)))' = f'(g(x)) · g'(x)"], ['outside derivative × inside derivative']),
           sc_steps(['d/dx (3x + 1)⁵', '= 5(3x + 1)⁴ · 3', '= 15(3x + 1)⁴'], ['', '', '']),
           sc_steps(['d/dx sin(x²)', '= cos(x²) · 2x'], ['', ''])],
  ['Differentiate the outside, keep the inside', 'Then multiply by the inside’s derivative'],
  [('d/dx (x² + 1)³ = ?', '6x(x² + 1)²', ['3(x² + 1)²', '6x']), ('d/dx e^(2x) = ?', '2e^(2x)', ['e^(2x)', '2x e^(2x)'])])
L('college', 'product_rule', 'Product and quotient rules', 'Derivatives of products',
  lambda: [sc_steps(["(uv)' = u'v + uv'", 'd/dx x² sin x', '= 2x sin x + x² cos x'], ['', '', '']),
           sc_steps(["(u/v)' = (u'v − uv')/v²", 'd/dx (x/(x + 1))', '= ((x + 1) − x)/(x + 1)²', '= 1/(x + 1)²'], ['', '', '', ''])],
  ["(uv)' = u'v + uv'", "(u/v)' = (u'v − uv')/v²"],
  [('d/dx (x · x²) = ?', '3x²', ['2x', 'x²']), ("(uv)' = ?", "u'v + uv'", ["u'v'", "u'v − uv'"])])
L('college', 'ftc', 'The fundamental theorem of calculus', 'Area from antiderivatives',
  lambda: [sc_riemann(lambda x: math.sin(x), 0, math.pi, (-0.3, 3.5), (-0.3, 1.3), ns=(4, 10, 30), exact='∫₀^π sin x dx = 2'),
           sc_steps(['∫ₐᵇ f(x) dx = F(b) − F(a)', '∫₀^π sin x dx = [−cos x]₀^π', '= 1 − (−1) = 2'], ["where F' = f", '', ''])],
  ['∫ₐᵇ f dx = F(b) − F(a)', 'Differentiation and integration are inverses'],
  [('∫₁² 2x dx = ?', '3', ['4', '2']), ('If F′ = f, then ∫ₐᵇ f dx = ?', 'F(b) − F(a)', ['F(a) − F(b)', 'f(b) − f(a)'])])
L('college', 'substitution', 'Integration by substitution', 'Undo the chain rule',
  lambda: [sc_steps(['∫ 2x (x² + 1)³ dx', 'u = x² + 1, du = 2x dx', '= ∫ u³ du = u⁴/4 + C', '= (x² + 1)⁴/4 + C'], ['', '', '', 'put x back']),
           sc_steps(['∫ cos(3x) dx', 'u = 3x, du = 3 dx', '= ⅓ sin(3x) + C'], ['', '', ''])],
  ['Pick u = the inside function', 'Swap dx for du, integrate, swap back'],
  [('∫ 2x e^(x²) dx = ?', 'e^(x²) + C', ['2e^(x²) + C', 'x²e^(x²) + C']), ('For ∫ sin(5x) dx, u = ?', '5x', ['sin x', '5'])])
L('college', 'matrices', 'Matrix multiplication', 'Rows times columns',
  lambda: [sc_matmul([[1, 2], [3, 4]], [[5, 6], [7, 8]]), sc_matmul([[2, 0], [1, 3]], [[1, 4], [2, 1]])],
  ['Entry (i, j) = row i · column j', 'AB is usually not BA'],
  [('[1 0; 0 1] × A = ?', 'A', ['0', '2A']), ('Can a 2×3 times a 3×2 be done?', 'yes', ['no', 'only if square'])])
L('college', 'determinants', 'Determinants', 'One number that says a lot',
  lambda: [sc_det([[3, 1], [2, 4]]), sc_det([[2, 6], [1, 3]]), sc_steps(['det = 0', '→ no inverse', '→ squashes area to 0'], ['', '', ''])],
  ['det [a b; c d] = ad − bc', 'det = 0 means no inverse'],
  [('det [2 1; 1 1] = ?', '1', ['3', '0']), ('det [1 2; 2 4] = ?', '0', ['8', '−4'])])
L('college', 'vectors', 'Vectors and the dot product', 'Arrows with size and direction',
  lambda: [sc_vectors((3, 1), (2, 4)), sc_steps(['u · v = |u||v| cos θ', 'u · v = 0 → perpendicular'], ['', ''])],
  ['Add vectors tip to tail', 'u · v = u₁v₁ + u₂v₂'],
  [('(1, 2) · (3, 4) = ?', '11', ['10', '(3, 8)']), ('|(3, 4)| = ?', '5', ['7', '25'])])
L('college', 'diffeq', 'Differential equations', 'Equations about rates of change',
  lambda: [sc_steps(['dy/dt = k y', 'y = y₀ e^(kt)'], ['growth proportional to size', 'the solution']),
           sc_graph([(lambda x: math.exp(0.5 * x), G, 'k = 0.5'), (lambda x: math.exp(-0.5 * x), R, 'k = −0.5'), (lambda x: 1.0, D, 'k = 0')], (-1, 6), (-0.5, 8), notes=['populations, money,', 'radioactive decay'], step=(1, 1)),
           sc_steps(['dy/dx = 2x', 'y = x² + C'], ['', 'integrate both sides'])],
  ["y' = ky → y = y₀eᵏᵗ", 'Integrate to solve'],
  [("Solution of y' = 3y with y(0) = 2?", '2e^(3t)', ['3e^(2t)', 'e^(6t)']), ("Solve y' = 6x", '3x² + C', ['6 + C', '6x² + C'])])
L('college', 'taylor', 'Taylor series', 'Functions as infinite polynomials',
  lambda: [sc_graph([(math.sin, WH, 'sin x'), (lambda x: x, P, 'x'), (lambda x: x - x ** 3 / 6, B, 'x − x³/6'), (lambda x: x - x ** 3 / 6 + x ** 5 / 120, G, '+ x⁵/120')], (-4, 4), (-2, 2), step=(1, 0.5)),
           sc_steps(['eˣ = 1 + x + x²/2! + x³/3! + …', 'sin x = x − x³/3! + x⁵/5! − …'], ['', ''])],
  ['More terms → better fit', 'f(x) = Σ f⁽ⁿ⁾(0) xⁿ / n!'],
  [('First two terms of eˣ?', '1 + x', ['x + x²', '1 − x']), ('Taylor series of sin x starts with…', 'x', ['1', 'x²'])])
L('college', 'euler', "Euler's formula", 'e, i, π in one line',
  lambda: [sc_unitcircle(), sc_steps(['e^(iθ) = cos θ + i sin θ', 'θ = π:', 'e^(iπ) + 1 = 0'], ['', '', 'the most famous equation'])],
  ['e^(iθ) walks around the unit circle', 'e^(iπ) = −1'],
  [('e^(iπ) = ?', '−1', ['1', 'i']), ('e^(iπ/2) = ?', 'i', ['−1', '0'])])


TARGET = 57.0


def build(l):
    """intro + worked scenes + 'your turn' practice (from the quiz) until about a minute + recap"""
    acc = ACC[l['grade']]
    sc = [intro(l['title'], l['sub'], acc)] + l['scenes']()
    rc = recap(l['recap'], acc)
    for q, a, _ in l['quiz']:
        if sum(d for d, _ in sc) + rc[0] >= TARGET: break
        sc.append(sc_try(q, a, acc))
    return sc + [rc]


def get(id_): return [l for l in LESSONS if l['id'] == id_][0]
def cat_label(l): return GNAME[l['grade']]


if __name__ == '__main__':
    if sys.argv[1] == 'list':
        tot = 0
        for l in LESSONS:
            d = sum(x for x, _ in build(l)); tot += d
            print(f"{l['grade']:8s} {l['id']:20s} {d:5.1f}s")
        print(len(LESSONS), 'lessons', round(tot), 's')
    elif sys.argv[1] == 'stills':
        l = get(sys.argv[2]); os.makedirs('out', exist_ok=True)
        render(f"out/{l['id']}.mp4", build(l), cat_label(l), l['title'], ACC[l['grade']], preview=[float(x) for x in sys.argv[3].split(',')])
